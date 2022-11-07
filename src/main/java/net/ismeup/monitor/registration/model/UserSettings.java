package net.ismeup.monitor.registration.model;

import net.ismeup.monitor.model.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UserSettings {
    private SystemMonitor selectedMonitor;
    private String name;
    private String bindTo;
    private int port;
    private String remoteAddress;
    private String key;
    private Map<String, String> mountPoints = new HashMap<>();
    private Server server;

    public UserSettings(SystemMonitor monitor, Server server) {
        selectedMonitor = monitor;
        name = selectedMonitor.getName();
        name = selectedMonitor.getName();
        remoteAddress = selectedMonitor.getHost();
        key = selectedMonitor.getKey();
        port = selectedMonitor.getPort();
        this.server = server;
    }

    public void parseConfiguration(Configuration configuration) {
        key = configuration.getKey();
        bindTo = configuration.getBind();
        port = configuration.getPort();
        Set<String> diskNames = configuration.getMountPoints();
        if (diskNames.size() > 0) {
            for (String mountPointName : diskNames) {
                mountPoints.put(mountPointName, configuration.getMountPointByName(mountPointName));
            }
        }
    }

    public SystemMonitor getSelectedMonitor() {
        return selectedMonitor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBindTo() {
        return bindTo;
    }

    public void setBindTo(String bindTo) {
        this.bindTo = bindTo;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Map<String, String> getMountPoints() {
        return mountPoints;
    }

    public Server getServer() {
        return server;
    }

    public Configuration getConfiguration() {
        Configuration configuration = Configuration.empty();
        configuration.setPort(port);
        configuration.setBind(bindTo);
        configuration.setKey(key);
        configuration.setDisks(mountPoints);
        return configuration;
    }

    public SystemMonitor getSystemMonitor() {
        SystemMonitor systemMonitor = SystemMonitor.fromSettings(selectedMonitor.getId(), name, remoteAddress, port, key);
        return systemMonitor;
    }
}
