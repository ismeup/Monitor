package net.ismeup.monitor.registration.model;

import net.ismeup.apiclient.model.CmdSelectable;
import org.json.JSONObject;

public class Server implements CmdSelectable {
    private int id;
    private String name;
    private String host;
    private ServerOwnType type;
    private double balance;
    private ServerWatcher defaultServerWatcher;

    public static Server empty() {
        Server server = new Server();
        server.id = 0;
        server.name = "";
        server.host = "";
        server.type = ServerOwnType.VIEWER;
        server.balance = .0;
        server.defaultServerWatcher = ServerWatcher.empty();
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

    public double getBalance() {
        return balance;
    }

    public ServerWatcher getDefaultServerWatcher() {
        return defaultServerWatcher;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject
                .put("id", id)
                .put("name", name)
                .put("host", host)
                .put("type", ServerOwnType.fromEnum(type))
                .put("balance", balance)
                .put("defaultServerWatcher", defaultServerWatcher.toJson())
                ;
        return jsonObject;
    }

    public static Server fromJson(JSONObject jsonObject) {
        Server server = new Server();
        server.id = jsonObject.optInt("id", 0);
        server.name = jsonObject.optString("name", "");
        server.host = jsonObject.optString("host", "");
        server.type = ServerOwnType.fromInt(jsonObject.optInt("type", 0));
        server.balance = jsonObject.optDouble("balance", .0);
        server.defaultServerWatcher = ServerWatcher.fromJson(jsonObject.optJSONObject("defaultServerWatcher", new JSONObject()));
        return server;
    }
}
