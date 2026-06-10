package net.ismeup.monitor.registration.model;

import net.ismeup.apiclient.model.CmdSelectable;
import org.json.JSONObject;

public class Server implements CmdSelectable {
    private int id;
    private String name;
    private String host;
    private ServerOwnType type;
    private ServerAgent defaultServerAgent;
    private boolean isCategory;

    public static Server empty() {
        Server server = new Server();
        server.id = 0;
        server.name = "";
        server.host = "";
        server.type = ServerOwnType.VIEWER;
        server.defaultServerAgent = ServerAgent.empty();
        server.isCategory = false;
        return server;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(CmdSelectable cmdSelectable) {
        return cmdSelectable instanceof Server && ((Server) cmdSelectable).id == id;
    }

    public String getHost() {
        return host;
    }

    public ServerOwnType getType() {
        return type;
    }

    public ServerAgent getDefaultServerAgent() {
        return defaultServerAgent;
    }

    public boolean isCategory() {
        return isCategory;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject
                .put("id", id)
                .put("name", name)
                .put("host", host)
                .put("type", ServerOwnType.fromEnum(type))
                .put("defaultServerWatcher", defaultServerAgent.toJson())
                .put("isCategory", isCategory)
                ;
        return jsonObject;
    }

    public static Server fromJson(JSONObject jsonObject) {
        Server server = new Server();
        server.id = jsonObject.optInt("id", 0);
        server.name = jsonObject.optString("name", "");
        server.host = jsonObject.optString("host", "");
        server.type = ServerOwnType.fromInt(jsonObject.optInt("type", 0));
        server.defaultServerAgent = ServerAgent.fromJson(jsonObject.optJSONObject("defaultServerWatcher", new JSONObject()));
        server.isCategory = jsonObject.optBoolean("isCategory", false);
        return server;
    }
}
