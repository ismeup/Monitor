package net.ismeup.monitor.controller;

import net.ismeup.monitor.model.CustomCheck;
import net.ismeup.monitor.model.LoadAverageType;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;

import java.io.*;
import java.util.Scanner;

public class Monitor {

    private SystemInfo systemInfo = new SystemInfo();

    public Monitor() { }

    public double getLoadAverage(LoadAverageType type) {
        double[] la = systemInfo.getHardware().getProcessor().getSystemLoadAverage(3);
        double retVal = .0;
        switch (type) {
            case ONE_MINUTE:
                retVal = la[0];
                break;
            case FIVE_MINUTE:
                retVal = la[1];
                break;
            case FIFTEEN_MINUTES:
                retVal = la[2];
                break;
        }
        return retVal;
    }

    public long getUptime() {
        return systemInfo.getOperatingSystem().getSystemUptime();
    }

    public int getMemUsagePercent() {
        HardwareAbstractionLayer hal = systemInfo.getHardware();
        long total = hal.getMemory().getTotal();
        long available = hal.getMemory().getAvailable();
        long used = total - available;
        int percentUsed = (int) ( (double) 100 / ((double) total / (double) used) );
        return percentUsed;
    }

    public int getDiskFreeSpace(String path) {
        File file = new File(path);
        long totalRootBytes = file.getTotalSpace();
        long freeRootBytes = file.getUsableSpace();
        double rootPercentUsed = 100d - 100d / ( (double) totalRootBytes / (double) freeRootBytes );
        return (int) Math.round(rootPercentUsed);
    }

    public boolean runBooleanCheck(CustomCheck customCheck) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", customCheck.getCommand()});
        process.waitFor();
        return process.exitValue() == 0;
    }

    public double runDoubleCheck(CustomCheck customCheck) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", customCheck.getCommand()});
        process.waitFor();
        try (Scanner scanner = new Scanner(process.getInputStream())) {
            if (scanner.hasNext()) {
                return Double.parseDouble(scanner.next());
            }
        } catch (NumberFormatException e) {
            // fall through
        }
        return -1d;
    }
}
