package com.aspegrenide.bt_mobile_server;

public class VideoCallMeta {
    private String currentToken;


    public VideoCallMeta() {

    }

    public VideoCallMeta(String token) {
        this.currentToken = token;
    }

    @Override
    public String toString() {
        return "VideoCallMeta{" +
                "token='" + currentToken + '\'' +
                '}';
    }

    public String getCurrentToken() {
        return currentToken;
    }

    public void setCurrentToken(String currentToken) {
        this.currentToken = currentToken;
    }
}
