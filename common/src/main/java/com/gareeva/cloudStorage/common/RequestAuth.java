package com.gareeva.cloudStorage.common;

public class RequestAuth extends AbstractMessage {
    private String login;
    private String password;

    public RequestAuth(String login, String password) {
        this.login = login;
        this.password = password;
    }

    @Override
    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }
}
