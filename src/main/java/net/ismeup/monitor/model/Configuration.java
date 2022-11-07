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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Configuration {
    private String key;
    private int port;
    private String bind;
    private Map<String, String> disks;

    public static Configuration empty() {
        Configuration configuration = new Configuration();
        configuration.key = "";
        configuration.port = 0;
        configuration.bind = "";
        configuration.disks = new HashMap<>();
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
}
