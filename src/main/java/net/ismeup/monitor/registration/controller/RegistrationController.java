package net.ismeup.monitor.registration.controller;

import net.ismeup.apiclient.controller.ApiConnector;
import net.ismeup.apiclient.controller.CmdLineInput;
import net.ismeup.apiclient.controller.OneTimeTokenStorage;
import net.ismeup.apiclient.exceptions.ConnectionFailException;
import net.ismeup.apiclient.model.ApiConnectionData;

import net.ismeup.apiclient.model.ApiResult;
import net.ismeup.apiclient.model.LoginData;
import net.ismeup.monitor.controller.StartController;
import net.ismeup.monitor.exceptions.AesException;
import net.ismeup.monitor.exceptions.CantParseConfigFile;
import net.ismeup.monitor.exceptions.CantReadConfigFile;
import net.ismeup.monitor.model.Configuration;
import net.ismeup.monitor.registration.model.*;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.webgrozny.simplehttpserver.exceptions.ServerBindException;
import ru.webgrozny.simplehttpserver.exceptions.SslInitException;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class RegistrationController {

    private final int defaultPort = 5555;

    private CmdLineInput cmdLineInput;
    private ApiConnector apiConnector;

    public void start(String[] arguments) {
        init(arguments);
        LoginData loginData = cmdLineInput.authenticate("Monitor", 3600);
        try {
            if (apiConnector.authenticate(loginData)) {
                System.out.println("Logged in");
                Server server = selectServer();
                System.out.println("Selected server: " + server.getName() + " (" + server.getHost() + ")");
                List<SystemMonitor> monitors = loadMonitors(server);
                SystemMonitor systemMonitor = null;
                if (monitors.size() > 0) {
                    systemMonitor = selectMonitor(monitors);
                } else {
                    System.out.println("There is no added Monitors, so we will create new one");
                    systemMonitor = SystemMonitor.empty();
                }
                if (systemMonitor.getId() == 0) {
                    systemMonitor.setName("");
                }
                UserSettings userSettings = new UserSettings(systemMonitor, server);
                try {
                    Configuration configuration = Configuration.fromConfig();
                    userSettings.parseConfiguration(configuration);
                    System.out.println("Found existing configuration");
                } catch (CantParseConfigFile e) {

                } catch (FileNotFoundException e) {

                } catch (CantReadConfigFile e) {

                }
                boolean connectionIsTested = false;
                while (!connectionIsTested) {
                    startRequestingData(userSettings);
                    connectionIsTested = testConnection(userSettings);
                }
                saveMonitorOnServer(userSettings);
                saveConfigurationFile(userSettings);
            } else {
                System.out.println("Login or password mismatch. Try again");
            }
        } catch (ConnectionFailException e) {

        }
    }

    private void saveMonitorOnServer(UserSettings userSettings) {
        ApiResult apiResult = apiConnector.postOperation(
                "system_monitor",
                "save_monitor",
                new JSONObject()
                        .put("server", userSettings.getServer().toJson())
                        .put("monitor", userSettings.getSystemMonitor().toJson())
        );
        if (apiResult.isOk()) {
            System.out.println("Monitor registered");
        }
    }

    private void saveConfigurationFile(UserSettings userSettings) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(userSettings.getConfiguration().configFilePath());
            fileOutputStream.write(userSettings.getConfiguration().getJson().toString().getBytes());
            fileOutputStream.close();
            System.out.println("Configuration file saved. Now you can run this monitor as usual");
        } catch (Exception e) {
            System.out.println("Can't save configuration file! You can save it manually as config.json");
            boolean print = cmdLineInput.requestYesNo("Do you want to print file content?", true);
            if (print) {
                System.out.println();
                System.out.println();
                System.out.println(userSettings.getConfiguration().getJson());
            }
        }
    }

    private boolean testConnection(UserSettings userSettings) {
        boolean result = false;
        boolean wantToTest = cmdLineInput.requestYesNo("Do you want to test this Monitor?", true);
        if (wantToTest) {
            List<ServerWatcher> serverWatcherList = new ArrayList<>();
            ApiResult apiResult = apiConnector.postOperation("server_watchers", "get_watchers");
            if (apiResult.isOk() && apiResult.getAnswer().optJSONArray("watchers").length() > 0) {
                for (Object jsonObject : apiResult.getAnswer().optJSONArray("watchers")) {
                    if (jsonObject instanceof JSONObject) {
                        serverWatcherList.add(ServerWatcher.fromJson((JSONObject) jsonObject));
                    }
                }
            }
            ServerWatcher serverWatcher = null;
            if (serverWatcherList.size() > 1) {
                serverWatcher = (ServerWatcher) cmdLineInput.requestSelectable("Select watcher to connect to this monitor", serverWatcherList, userSettings.getServer().getDefaultServerWatcher());
            } else {
                serverWatcher = serverWatcherList.get(0);
            }
            if (serverWatcher != null) {
                StartController startController = new StartController();
                System.out.println("Starting Monitoring server...");
                AtomicBoolean isOk = new AtomicBoolean(true);
                new Thread(
                        () -> {
                            try {
                                startController.startFromConfiguration(userSettings.getConfiguration());
                            } catch (AesException e) {
                                System.out.println("Can't init AES Encryption cipher. Possible bad AES-key");
                                isOk.set(false);
                            } catch (ServerBindException e) {
                                System.out.println("Can't bind to " + userSettings.getPort() + ". Try to change port");
                                isOk.set(false);
                            } catch (SslInitException e) {
                                System.out.println("Must not be thrown");
                                isOk.set(false);
                            }
                        }
                ).start();
                boolean testPassed = false;
                System.out.println("Waiting 3 secs");
                if (isOk.get()) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    ApiResult testResult = apiConnector.postOperation(
                            "system_monitor",
                            "test_monitor",
                            new JSONObject()
                                    .put("serverWatcher", serverWatcher.toJson())
                                    .put("systemMonitor", userSettings.getSystemMonitor().toJson())
                                    .put("server", userSettings.getServer().toJson())
                    );
                    startController.stop();
                    testPassed = testResult.isOk();
                }
                if (testPassed) {
                    result = true;
                } else {
                    System.out.println("Test failed!");
                    result = cmdLineInput.requestYesNo("Continue anyway?", false);
                }

            }
        } else {
            result = true;
        }
        return result;
    }

    private void startRequestingData(UserSettings userSettings) {
        boolean yes = false;
        while (!yes) {
            userSettings.setName(cmdLineInput.requestString("Enter name", userSettings.getName().isEmpty() ? null : userSettings.getName()));
            getBindInterface(userSettings);
            userSettings.setPort(cmdLineInput.requestInt("Enter port (1 - 65535) ", userSettings.getPort() == 0 ? defaultPort : userSettings.getPort()));
            while (userSettings.getPort() < 1 || userSettings.getPort() > 65535) {
                userSettings.setPort(cmdLineInput.requestInt("Enter port", userSettings.getPort() == 0 ? defaultPort : userSettings.getPort()));
            }
            setRemoteIp(userSettings);
            userSettings.setKey(cmdLineInput.requestString("Enter AES key", userSettings.getKey().isEmpty() ? null : userSettings.getKey()));
            printDisks(userSettings);
            printSummary(userSettings);
            yes = cmdLineInput.requestYesNo("Is above data right?", true);
        }
    }

    private void init(String[] arguments) {
        this.cmdLineInput = new CmdLineInput();
        ApiConnectionData apiConnectionData = ApiConnectionData.defaultUrl();
        if (arguments.length > 1) {
            try {
                apiConnectionData = ApiConnectionData.parse(arguments[1]);
            } catch (MalformedURLException e) {
                System.out.println("Can't parse registration URL");
            }
        }
        apiConnector = new ApiConnector(apiConnectionData, new OneTimeTokenStorage());
    }

    private void printSummary(UserSettings userSettings) {
        System.out.println();
        System.out.println("====== Summary ======");
        System.out.println();
        if (userSettings.getSelectedMonitor().getId() == 0) {
            System.out.println("You are going to create Monitor");
        } else {
            System.out.println("You are going to configure existsing Monitor");
        }
        System.out.println("Name: " + userSettings.getName());
        System.out.println("Remote address: " + userSettings.getRemoteAddress());
        System.out.println("Bind port: " + userSettings.getPort());
        System.out.println("Bind address: " + userSettings.getBindTo());
        System.out.println("AES Encryption key: " + userSettings.getKey());
        System.out.println("Disks:");
        userSettings.getMountPoints().keySet().forEach( e -> {
            System.out.println("    " + e + " : " + userSettings.getMountPoints().get(e));
        } );
        System.out.println();
        System.out.println("====== Configuration file (config.json) ======");
        System.out.println();
        System.out.println(userSettings.getConfiguration().getJson().toString());
        System.out.println();

    }

    private Server selectServer() {
        Server server = Server.empty();
        List<Server> serverList = new ArrayList<>();
        ApiResult apiResult = apiConnector.postOperation("servers", "get_servers");
        if (apiResult.isOk() && !apiResult.isConnectionInterrupted() && apiResult.getAnswer().optJSONArray("servers").length() > 0) {
            JSONArray jsonArray = apiResult.getAnswer().getJSONArray("servers");
            for (Object jsonObject : jsonArray) {
                if (jsonObject instanceof JSONObject) {
                    serverList.add(Server.fromJson((JSONObject) jsonObject));
                }
            }
        }
        server = (Server) cmdLineInput.requestSelectable("Select server", serverList);
        return server;
    }

    private void getBindInterface(UserSettings userSettings) {
        BindInterface selectedBindInterface = null;
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            List<BindInterface> bindInterfaces = new ArrayList<>();
            List<InetAddress> inetAddresses = new ArrayList<>();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAddressEnumeration = networkInterface.getInetAddresses();
                while (inetAddressEnumeration.hasMoreElements()) {
                    InetAddress inetAddress = inetAddressEnumeration.nextElement();
                    inetAddresses.add(inetAddress);
                }
            }
            for (InetAddress current : inetAddresses) {
                byte[] address = current.getAddress();
                int addr1 = Byte.toUnsignedInt(address[0]);
                int addr2 = Byte.toUnsignedInt(address[1]);
                int addr3 = Byte.toUnsignedInt(address[2]);
                int addr4 = Byte.toUnsignedInt(address[3]);
                String ipAddress = addr1 + "." + addr2 + "." + addr3 + "." + addr4;
                BindInterface bindInterface = new BindInterface(ipAddress);
                if (!bindInterfaces.contains(bindInterface)) {
                    bindInterfaces.add(bindInterface);
                }
            }
            bindInterfaces.sort(Comparator.comparing(BindInterface::getName));
            BindInterface defaultBindInterface = userSettings.getBindTo() == null ? new BindInterface("0.0.0.0") : new BindInterface(userSettings.getBindTo());
            selectedBindInterface = (BindInterface) cmdLineInput.requestSelectable("Where bind to?", bindInterfaces, defaultBindInterface);
            userSettings.setBindTo(selectedBindInterface.getName());
        } catch (SocketException e) {

        }
    }

    private List<SystemMonitor> loadMonitors(Server server) {
        List<SystemMonitor> monitorList = new ArrayList<>();
        ApiResult apiResult = apiConnector.postOperation("system_monitor", "get_monitors", new JSONObject().put("server", server.toJson()));
        if (apiResult.isOk()) {
            JSONArray monitorsJson = apiResult.getAnswer().optJSONArray("monitors");
            for (Object current : monitorsJson) {
                if (current instanceof JSONObject) {
                    monitorList.add(SystemMonitor.fromJson((JSONObject) current));
                }
            }
        }
        return monitorList;
    }

    private SystemMonitor selectMonitor(List<SystemMonitor> monitors) {
        SystemMonitor newMonitor = SystemMonitor.empty();
        newMonitor.setName("<Create new monitor>");
        monitors.add(0, newMonitor);
        SystemMonitor monitor = (SystemMonitor) cmdLineInput.requestSelectable("Select monitor", monitors);
        return monitor;
    }

    private void setRemoteIp(UserSettings userSettings) {
        String defaultIpAddress = userSettings.getRemoteAddress();
        if (defaultIpAddress == null || defaultIpAddress.isEmpty()) {
            ApiResult apiResult = apiConnector.postOperation("remote_address", "get_ip");
            defaultIpAddress = apiResult.getAnswer().optString("ip", "");
        }
        userSettings.setRemoteAddress(cmdLineInput.requestString("Remote IP address", defaultIpAddress.isEmpty() ? null : defaultIpAddress));
    }

    private void printDisks(UserSettings userSettings) {
        System.out.println("Configured disks:");
        String operation = "";
        while (!operation.equals("c")) {
            int i = 0;
            if (userSettings.getMountPoints().size() <= 0) {
                System.out.println("Disks not added");
            }
            for (String name : userSettings.getMountPoints().keySet()) {
                System.out.println("[" + i + "] " + name + " : " + userSettings.getMountPoints().get(name));
                i++;
            }
            System.out.println("Enter [a] to add disk");
            System.out.println("      [m] to edit disk");
            System.out.println("      [r] to remove disk");
            System.out.println("      [s] to scan disks");
            System.out.println("      [c] to continue");
            operation = cmdLineInput.requestString("Select operation", "c");
            switch (operation) {
                case "a":
                    addDisk(userSettings);
                    break;
                case "m":
                    editDisk(userSettings);
                    break;
                case "r":
                    removeDisk(userSettings);
                    break;
                case "s":
                    scanDisks(userSettings);
                    break;
            }
        }
    }

    private void addDisk(UserSettings userSettings) {
        long freeSpace = 0;
        String diskPath = null;
        while (freeSpace == 0) {
            diskPath = cmdLineInput.requestString("Enter path");
            File file = new File(diskPath);
            freeSpace = file.getFreeSpace();
            if (freeSpace == 0) {
                System.out.println("Disk does not exists");
            }
        }
        String defaultName = getPathAlias(diskPath);
        String diskAlias = cmdLineInput.requestString("Enter disk alias", defaultName);
        userSettings.getMountPoints().put(diskAlias, diskPath);
    }

    private String getPathAlias(String path) {
        String defaultName = null;
        String diskPathFormatted = path.replace("\\", "/").replace(":", "/");
        if (diskPathFormatted.equals("/")) {
            defaultName = "root";
        } else {
            String[] elements = diskPathFormatted.split("/");
            defaultName = elements[elements.length - 1];
        }
        return defaultName;
    }

    private void removeDisk(UserSettings userSettings) {
            int value = -1;
            while (value == -1) {
                String diskNumberString = cmdLineInput.requestString("Enter disk number to remove or [c] to cancel");
                if (diskNumberString.equals("c")) {
                    return;
                }
                int newValue;
                try {
                    newValue = Integer.parseInt(diskNumberString);
                    value = newValue;
                    int i = 0;
                    for (String current : userSettings.getMountPoints().keySet()) {
                        if (i == value) {
                            userSettings.getMountPoints().remove(current);
                            break;
                        }
                        i++;
                    }
                    userSettings.getMountPoints().keySet();
                } catch (NumberFormatException e) {

                }
            }

    }

    private void editDisk(UserSettings userSettings) {
        int value = -1;
        while (value == -1) {
            String diskNumberString = cmdLineInput.requestString("Enter disk number to edit or [c] to cancel");
            if (diskNumberString.equals("c")) {
                return;
            }
            int newValue;
            try {
                newValue = Integer.parseInt(diskNumberString);
                value = newValue;
                int i = 0;
                Set<String> diskAliases = userSettings.getMountPoints().keySet();
                for (String current : diskAliases) {
                    if (i == value) {
                        long freeSpace = 0;
                        String diskPath = null;
                        while (freeSpace == 0) {
                            diskPath = cmdLineInput.requestString("Enter path", userSettings.getMountPoints().get(current));
                            File file = new File(diskPath);
                            freeSpace = file.getFreeSpace();
                            if (freeSpace == 0) {
                                System.out.println("Disk does not exists");
                            }
                        }
                        String defaultName = current;
                        String diskAlias = cmdLineInput.requestString("Enter disk alias", defaultName);
                        userSettings.getMountPoints().remove(current);
                        userSettings.getMountPoints().put(diskAlias, diskPath);
                        break;
                    }
                    i++;
                }
            } catch (NumberFormatException e) {

            }
        }
    }

    private void scanDisks(UserSettings userSettings) {
        try {
            FileInputStream fileInputStream = new FileInputStream("/etc/fstab");
            int size = fileInputStream.available();
            byte[] b = new byte[size];
            fileInputStream.read(b);
            String[] str = new String(b).replace("\r\n", "--newline--").replace("\r", "--newline--").replace("\n", "--newline--").split("--newline--");
            for (String current : str) {
                if (!current.trim().startsWith("#")) {
                    //System.out.println(current);
                    String currentString = current.trim().replace("\t", " ");
                    while (currentString.contains("  ")) {
                        currentString = currentString.replace("  ", " ");
                    }
                    String[] pathElements = currentString.split(" ");
                    if (pathElements.length >= 2) {
                        String path = pathElements[1];
                        String name = getPathAlias(path);
                        if (name != null) {
                            userSettings.getMountPoints().put(name, path);
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Can't find file /etc/fstab. Please, add disk manually");
        } catch (IOException e) {
            System.out.println("Can't read file /etc/fstab. Please, add disk manually");
        }
    }

}
