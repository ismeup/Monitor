package net.ismeup.monitor.model;

import net.ismeup.monitor.Main;
import net.ismeup.monitor.exceptions.CantParseConfigFile;
import net.ismeup.monitor.exceptions.CantReadConfigFile;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Configuration {
    private String key;
    private int port;
    private String bind;
    private Map<String, String> disks;
    private List<CustomCheck> customChecks;

    public static Configuration empty() {
        Configuration configuration = new Configuration();
        configuration.key = "";
        configuration.port = 0;
        configuration.bind = "";
        configuration.disks = new HashMap<>();
        configuration.customChecks = new ArrayList<>();
        return configuration;
    }

    public static Configuration fromConfig() throws CantParseConfigFile, FileNotFoundException, CantReadConfigFile {
        Configuration configuration = new Configuration();
        configuration.readConfigurationFile();
        return configuration;
    }

    private Configuration() {
        //readConfigurationFile();
    }

    private void readConfigurationFile() throws FileNotFoundException, CantReadConfigFile, CantParseConfigFile {
        try {
            String inputFilePath = configFilePath();
            FileInputStream inStream = new FileInputStream(inputFilePath);
            int size = inStream.available();
            byte[] data = new byte[size];
            inStream.read(data);
            String configurationFile = new String(data);
            parseConfiguration(configurationFile);
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException();
        } catch (URISyntaxException e) {
            throw new CantReadConfigFile();
        } catch (IOException e) {
            throw new CantReadConfigFile();
        }
    }

    public String configFilePath() throws URISyntaxException {
        String fileName = "config.json";
        File jarFile = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
        String inputFilePath = jarFile.getParent() + File.separator + fileName;
        return inputFilePath;
    }

    private void parseConfiguration(String configurationFile) throws CantParseConfigFile {
        try {
            JSONObject jsonObject = new JSONObject(configurationFile);
            key = jsonObject.getString("key");
            bind = jsonObject.getString("bind");
            port = Integer.parseInt(jsonObject.get("port").toString());
            disks = new HashMap<>();
            JSONArray mountPoints = jsonObject.optJSONArray("mount_points");
            if (mountPoints != null) {
                for (Object mountPointObject : mountPoints) {
                    if (mountPointObject instanceof JSONObject) {
                        String name = ((JSONObject) mountPointObject).getString("name");
                        String path = ((JSONObject) mountPointObject).getString("path");
                        disks.put(name, path);
                    } else {
                        throw new CantParseConfigFile();
                    }
                }
            }
            customChecks = new ArrayList<>();
            JSONArray customChecksArray = jsonObject.optJSONArray("custom_checks");
            if (customChecksArray != null) {
                for (Object checkObject : customChecksArray) {
                    if (checkObject instanceof JSONObject) {
                        JSONObject checkJson = (JSONObject) checkObject;
                        String name = checkJson.getString("name");
                        String command = checkJson.getString("command");
                        String typeStr = checkJson.getString("type").toUpperCase();
                        CustomCheckType type = CustomCheckType.valueOf(typeStr);
                        customChecks.add(CustomCheck.of(name, type, command));
                    } else {
                        throw new CantParseConfigFile();
                    }
                }
            }
        } catch (Exception e) {
            throw new CantParseConfigFile();
        }
    }

    public JSONObject getJson() {
        JSONObject jsonObject = new JSONObject();
        JSONArray disksArray = new JSONArray();
        disks.forEach( (disk, mountPoint) -> {
            disksArray.put(new JSONObject().put("name", disk).put("path", mountPoint));
        } );
        jsonObject.put("key", key);
        jsonObject.put("bind", bind);
        jsonObject.put("port", port);
        jsonObject.put("mount_points", disksArray);
        JSONArray checksArray = new JSONArray();
        for (CustomCheck check : customChecks) {
            checksArray.put(new JSONObject()
                    .put("name", check.getName())
                    .put("type", check.getType().name().toLowerCase())
                    .put("command", check.getCommand()));
        }
        jsonObject.put("custom_checks", checksArray);

        return jsonObject;
    }

    public String getKey() {
        return key;
    }

    public String getBind() {
        return bind;
    }

    public int getPort() {
        return port;
    }

    public Set<String> getMountPoints() {
        return disks.keySet();
    }

    public String getMountPointByName(String disk) {
        return disks.get(disk);
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setBind(String bind) {
        this.bind = bind;
    }

    public void setDisks(Map<String, String> disks) {
        this.disks = disks;
    }

    public void setCustomChecks(List<CustomCheck> customChecks) {
        this.customChecks = customChecks;
    }

    public CustomCheck getCustomCheckByName(String name) {
        for (CustomCheck check : customChecks) {
            if (check.getName().equals(name)) {
                return check;
            }
        }
        return null;
    }

    public List<CustomCheck> getCustomChecks() {
        return customChecks;
    }
}
