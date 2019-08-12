package com.gareeva.cloudStorage.common;

public class ReportFileExists extends AbstractMessage {
    private String fileName;
    private boolean fileExists;

    public ReportFileExists(String fileName, boolean fileExists) {
        this.fileName = fileName;
        this.fileExists = fileExists;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isFileExists() {
        return fileExists;
    }
}
