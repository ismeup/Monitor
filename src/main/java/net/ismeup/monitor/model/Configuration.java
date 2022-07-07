package net.ismeup.monitor.model;

import net.ismeup.monitor.Main;
import net.ismeup.monitor.exceptions.CantParseConfigFile;
import net.ismeup.monitor.exceptions.CantReadConfigFile;
import org.json.JSONArray;
import org.json.JSONException;
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

    public Configuration() throws CantParseConfigFile, FileNotFoundException, CantReadConfigFile {
        readConfigurationFile();
    }

    private void readConfigurationFile() throws FileNotFoundException, CantReadConfigFile, CantParseConfigFile {
        try {
            String fileName = "config.json";
            File jarFile = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            String inputFilePath = jarFile.getParent() + File.separator + fileName;
            FileInputStream inStream = new FileInputStream(new File(inputFilePath));
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
}
