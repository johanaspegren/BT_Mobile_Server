package com.aspegrenide.bt_mobile_server;

public class GoogleGlasses {
    private boolean connected;
    private String uid;

    public GoogleGlasses(boolean connected, String uid) {
        this.connected = connected;
        this.uid = uid;
    }

    public GoogleGlasses() {

    }

    @Override
    public String toString() {
        return "GoogleGlasses{" +
                "connected=" + connected +
                ", uid='" + uid + '\'' +
                '}';
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
