package com.gareeva.cloudStorage.common;

public class ReportErrorFileOperation extends AbstractMessage {
    private String fileName;
    private FileTypeOperation operation;

    public String getFileName() {
        return fileName;
    }

    public FileTypeOperation getOperation() {
        return operation;
    }

    public ReportErrorFileOperation(String fileName, FileTypeOperation operation) {
        this.fileName = fileName;
        this.operation = operation;

    }
}
