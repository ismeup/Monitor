package net.ismeup.monitor.controller;

import net.ismeup.monitor.Monitor;
import net.ismeup.monitor.model.Configuration;
import net.ismeup.monitor.exceptions.CantParseException;
import net.ismeup.monitor.model.LoadAverageType;
import org.json.JSONObject;
import ru.webgrozny.simplehttpserver.ContentProvider;
import ru.webgrozny.simplehttpserver.ServerStatus;

import java.util.Base64;
import java.util.UUID;

public class WebController extends ContentProvider  {

    private AesController aesController;
    private Configuration configuration;

    public WebController(AesController aesController, Configuration configuration) {
        this.aesController = aesController;
        this.configuration = configuration;
    }

    @Override
    public void execute() {
        String base64Encoded = getRawPost();
        try {
            JSONObject requestJson = decodeRequest(base64Encoded);
            String result = getJsonDataObject(requestJson);
            String encryptedResult = encodeAnswer(result);
            setHeader("Content-type: text/plain");
            echo(encryptedResult);
        } catch (Exception e) {
            e.printStackTrace();
            setHeader("Content-type: text/html");
            setAnswer(ServerStatus.NOT_FOUND);
            echo("<h1>404 Not found</h1>");
        }
    }

    private String getJsonDataObject(JSONObject request) throws CantParseException {
        String stringRequest = request.getString("operation");
        String answer = "";
        Monitor monitor = new Monitor();
        switch (stringRequest) {
            case "uptime" :
                answer = monitor.getUptime() + "";
                break;
            case "mem" :
                answer = monitor.getMemUsagePercent() + "";
                break;
            case "la_1":
                answer = monitor.getLoadAverage(LoadAverageType.ONE_MINUTE) + "";
                break;
            case "la_5":
                answer = monitor.getLoadAverage(LoadAverageType.FIVE_MINUTE) + "";
                break;
            case "la_15":
                answer = monitor.getLoadAverage(LoadAverageType.FIFTEEN_MINUTES) + "";
                break;
            case "disk" :
                try {
                    String mountPointName = request.getString("disk");
                    String disk = configuration.getMountPointByName(mountPointName);
                    if (disk == null) {
                        answer = "-1";
                    } else {
                        answer = monitor.getDiskFreeSpace(disk) + "";
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new CantParseException();
                }
                break;
            default:
                throw new CantParseException();
        }
        return answer;
    }

    private String encodeAnswer(String result) throws CantParseException {
        String answer = "";
        try {
            String saltedAnswer = new JSONObject().put("answer", result).put("salt", UUID.randomUUID().toString()).toString();
            System.out.println("Answer: " + saltedAnswer);
            byte[] encryptedData = aesController.encrypt(saltedAnswer.getBytes("UTF-8"));
            byte[] base64Answer = Base64.getEncoder().encode(encryptedData);
            answer = new String(base64Answer);
        } catch (Exception e) {
            throw new CantParseException();
        }
        return answer;
    }

    private JSONObject decodeRequest(String request) throws CantParseException {
        try {
            byte[] requestBytes = Base64.getDecoder().decode(request);
            byte[] decryptedData = aesController.decrypt(requestBytes);
            String decryptedString = new String(decryptedData);
            System.out.println("Request: " + decryptedString);
            return new JSONObject(decryptedString);
        } catch (Exception e) {
            System.out.println("Can't decrypt data! Possible wrong AES key is used");
            throw new CantParseException();
        }
    }
}
