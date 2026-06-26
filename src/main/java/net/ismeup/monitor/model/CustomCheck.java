package net.ismeup.monitor.model;

public class CustomCheck {
    private String name;
    private CustomCheckType type;
    private String command;

    public String getName() {
        return name;
    }

    public CustomCheckType getType() {
        return type;
    }

    public String getCommand() {
        return command;
    }

    public static CustomCheck of(String name, CustomCheckType type, String command) {
        CustomCheck check = new CustomCheck();
        check.name = name;
        check.type = type;
        check.command = command;
        return check;
    }
}
