package com.gareeva.cloudStorage.common;

public class RequestFileExists extends AbstractMessage {
    private String fileName;

    public RequestFileExists(String login, String fileName) {
        this.login = login;
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
