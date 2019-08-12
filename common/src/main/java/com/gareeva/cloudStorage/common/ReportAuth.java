package com.gareeva.cloudStorage.common;

public class ReportAuth extends AbstractMessage {
    private boolean success;

    public ReportAuth(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

}
