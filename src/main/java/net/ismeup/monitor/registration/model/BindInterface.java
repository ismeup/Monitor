package net.ismeup.monitor.registration.model;

import net.ismeup.apiclient.model.CmdSelectable;

public class BindInterface implements CmdSelectable {
    private String ip;

    public BindInterface(String ip) {
        this.ip = ip;
    }

    @Override
    public String getName() {
        return ip;
    }

    @Override
    public boolean equals(CmdSelectable cmdSelectable) {
        return cmdSelectable instanceof BindInterface && ((BindInterface) cmdSelectable).ip.equals(ip);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BindInterface && ((BindInterface) obj).ip.equals(ip);
    }
}
