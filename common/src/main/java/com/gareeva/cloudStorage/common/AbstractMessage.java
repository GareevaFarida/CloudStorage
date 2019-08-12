package com.gareeva.cloudStorage.common;

import java.io.Serializable;

public abstract class AbstractMessage implements Serializable {
    protected String login;

    public String getLogin() {
        return login;
    }
}