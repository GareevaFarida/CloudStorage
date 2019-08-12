package com.gareeva.cloudStorage.common;

import java.nio.file.Path;

/**
 * Сообщение со вложенным байтовым массивом - данными файла.
 * Клиент и сервер обмениваются файлами с помощью таких сообщений
 */
public class MessageFile extends AbstractMessage {
    private String filename;
    private byte[] data;
    private int numberOfPackage;
    private int totalPackages;

    public String getFilename() {
        return filename;
    }

    public byte[] getData() {
        return data;
    }

    public MessageFile(String login, Path path, byte[] data, int numberOfPackage, int totalPackages) {
        this.login = login;
        this.filename = path.getFileName().toString();
        this.data = data;
        this.numberOfPackage = numberOfPackage;
        this.totalPackages = totalPackages;
    }

    public boolean isLastPackage() {
        return numberOfPackage == totalPackages;
    }

    public int getNumberOfPackage() {
        return numberOfPackage;
    }

    public int getTotalPackages() {
        return totalPackages;
    }
}
