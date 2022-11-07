package net.ismeup.monitor.registration.model;

import net.ismeup.apiclient.model.CmdSelectable;
import org.json.JSONObject;

public class SystemMonitor implements CmdSelectable {
    private int id;
    private String name;
    private String host;
    private int port;
    private String key;

    private SystemMonitor() {

    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(CmdSelectable cmdSelectable) {
        return cmdSelectable instanceof SystemMonitor && ((SystemMonitor) cmdSelectable).id == id;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getKey() {
        return key;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject
                .put("id", id)
                .put("name", name)
                .put("host", host)
                .put("port", port)
                .put("key", key);
        return jsonObject;
    }

    public static SystemMonitor fromJson(JSONObject jsonObject) {
        SystemMonitor systemMonitor = new SystemMonitor();
        systemMonitor.id = jsonObject.optInt("id", 0);
        systemMonitor.name = jsonObject.optString("name", "");
        systemMonitor.host = jsonObject.optString("host", "");
        systemMonitor.port = jsonObject.optInt("port", 0);
        systemMonitor.key = jsonObject.optString("key", "");
        return systemMonitor;
    }

    public static SystemMonitor empty() {
        SystemMonitor systemMonitor = new SystemMonitor();
        systemMonitor.id = 0;
        systemMonitor.name = "";
        systemMonitor.host = "";
        systemMonitor.port = 0;
        systemMonitor.key = "";
        return systemMonitor;
    }

    public void setName(String s) {
        this.name = s;
    }

    public static SystemMonitor fromSettings(int id, String name, String host, int port, String key) {
        SystemMonitor systemMonitor = new SystemMonitor();
        systemMonitor.id = id;
        systemMonitor.name = name;
        systemMonitor.host = host;
        systemMonitor.port = port;
        systemMonitor.key = key;
        return systemMonitor;
    }
}
