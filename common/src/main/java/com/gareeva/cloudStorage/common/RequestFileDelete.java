package com.gareeva.cloudStorage.common;

public class RequestFileDelete extends AbstractMessage {
    private String fileName;

    public RequestFileDelete(String login, String fileName) {
        this.login = login;
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

}
