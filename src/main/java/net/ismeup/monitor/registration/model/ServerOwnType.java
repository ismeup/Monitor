package net.ismeup.monitor.registration.model;

public enum ServerOwnType {
    OWNER,
    EDITOR,
    VIEWER;

    public static ServerOwnType fromInt(int type) {
        ServerOwnType serverOwnType = null;
        switch (type) {
            case (1) :
                serverOwnType = OWNER;
                break;
            case (2) :
                serverOwnType = EDITOR;
                break;
            case (3) :
                serverOwnType = VIEWER;
                break;
            default:
                serverOwnType = VIEWER;
        }
        return serverOwnType;
    }

    public static int fromEnum(ServerOwnType serverOwnType) {
        int value = 0;
        switch (serverOwnType) {
            case OWNER:
                value = 1;
                break;
            case EDITOR:
                value = 2;
                break;
            case VIEWER:
                value = 3;
                break;
            default:
                value = 3;
        }
        return value;
    }
}
