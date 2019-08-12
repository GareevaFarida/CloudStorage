package com.gareeva.cloudStorage.common;

public class RequestRegistration extends AbstractMessage {
    private String login;
    private String password;

    public RequestRegistration(String login, String password) {
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
