package net.ismeup.monitor.registration.model;


import net.ismeup.apiclient.model.CmdSelectable;
import org.json.JSONObject;

public class ServerAgent implements CmdSelectable {
    private int id;
    private String name;
    private String key;
    private long lastOnline;
    private boolean isMain;
    private int checksCount;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(CmdSelectable cmdSelectable) {
        return cmdSelectable instanceof ServerAgent && ((ServerAgent) cmdSelectable).id == id;
    }

    public String getKey() {
        return key;
    }

    public long getLastOnline() {
        return lastOnline;
    }

    public boolean isMain() {
        return isMain;
    }

    public int getChecksCount() {
        return checksCount;
    }

    public JSONObject toJson() {
        return new JSONObject()
                .put("id", id)
                .put("name", name)
                .put("key", key)
                .put("lastOnline", lastOnline)
                .put("isMain", isMain)
                .put("checksCount", checksCount);
    }

    public static ServerAgent fromJson(JSONObject jsonObject) {
        ServerAgent serverAgent = new ServerAgent();
        serverAgent.id = jsonObject.optInt("id", 0);
        serverAgent.name = jsonObject.optString("name", "");
        serverAgent.key = jsonObject.optString("key", "");
        serverAgent.lastOnline = jsonObject.optLong("lastOnline", 0);
        serverAgent.isMain = jsonObject.optBoolean("isMain", false);
        serverAgent.checksCount = jsonObject.optInt("checksCount", 0);
        return serverAgent;
    }

    public static ServerAgent empty() {
        ServerAgent serverAgent = new ServerAgent();
        serverAgent.id = 0;
        serverAgent.name = "";
        serverAgent.key = "";
        serverAgent.lastOnline = 0;
        serverAgent.isMain = false;
        serverAgent.checksCount = 0;
        return serverAgent;
    }

    public static ServerAgent createByName(String name) {
        ServerAgent serverAgent = empty();
        serverAgent.name = name;
        return serverAgent;
    }
}
