package com.gareeva.cloudStorage.common;

public class ReportRegistration extends AbstractMessage {
    private boolean success;

    public ReportRegistration(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }
}
