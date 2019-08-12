package com.gareeva.cloudStorage.common;

public class RequestFile extends AbstractMessage {
    private String filename;

    public String getFilename() {
        return filename;
    }

    public RequestFile(String login, String filename) {
        this.login = login;
        this.filename = filename;
    }
}
