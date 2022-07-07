package net.ismeup.monitor;

import net.ismeup.monitor.controller.AesController;
import net.ismeup.monitor.controller.WebController;
import net.ismeup.monitor.exceptions.AesException;
import net.ismeup.monitor.exceptions.CantParseConfigFile;
import net.ismeup.monitor.exceptions.CantReadConfigFile;
import net.ismeup.monitor.model.Configuration;
import net.ismeup.monitor.model.LoadAverageType;
import ru.webgrozny.simplehttpserver.Server;
import ru.webgrozny.simplehttpserver.ServerSettings;
import ru.webgrozny.simplehttpserver.exceptions.ServerBindException;
import ru.webgrozny.simplehttpserver.exceptions.SslInitException;

import java.io.FileNotFoundException;

public class Main {
    public static void main(String[] args) {
        try {
            Configuration configuration = new Configuration();
            Monitor monitor = new Monitor();
            System.out.println("Current memory usage: " + monitor.getMemUsagePercent() + "%");
            System.out.println("Uptime: " + monitor.getUptime() + " seconds");
            System.out.println("Load Avg (1 min): " + monitor.getLoadAverage(LoadAverageType.ONE_MINUTE));
            System.out.println("Load Avg (5 min): " + monitor.getLoadAverage(LoadAverageType.FIVE_MINUTE));
            System.out.println("Load Avg (15 min): " + monitor.getLoadAverage(LoadAverageType.FIFTEEN_MINUTES));
            System.out.println("Disks usage");
            configuration.getMountPoints().forEach( (String disk) -> {
                System.out.println("\t" + disk + ": " + monitor.getDiskFreeSpace(configuration.getMountPointByName(disk)) + "%");
            } );
            System.out.println("Starting monitoring daemon at " + configuration.getBind() + ":" + configuration.getPort());
            AesController aesController = new AesController(configuration.getKey());
            ServerSettings serverSettings = ServerSettings
                    .createDefaultConfig()
                    .setPort(configuration.getPort())
                    .setBindTo(configuration.getBind())
                    .disableDocumentRoot()
                    .setProviderGenerator(() -> new WebController(aesController, configuration));
            new Server(serverSettings).start();
        } catch (AesException e) {
            System.out.println("Can't init aes ciphers");
            System.exit(1);
        } catch (ServerBindException e) {
            System.out.println("Can't bind server to " + e.getBindTo() + ":" + e.getPort());
            System.out.println("Possible wrong configuration");
            System.exit(1);
        } catch (SslInitException e) {
            //We are not using SSL
        } catch (CantParseConfigFile e) {
            System.out.println("Can't parse configuration file. Possible wrong configuration");
        } catch (FileNotFoundException e) {
            System.out.println("config.json is not found");
        } catch (CantReadConfigFile e) {
            System.out.println("Can't read configuration file");
        }
    }
}
