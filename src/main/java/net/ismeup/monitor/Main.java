package net.ismeup.monitor;

import net.ismeup.monitor.controller.*;
import net.ismeup.monitor.registration.controller.RegistrationController;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--setup")) {
            new RegistrationController().start(args);
        } else {
            new StartController().start();
        }
    }
}
