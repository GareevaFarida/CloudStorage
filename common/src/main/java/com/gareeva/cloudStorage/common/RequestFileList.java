package com.gareeva.cloudStorage.common;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RequestFileList extends AbstractMessage {
    private ArrayList<String> fileList;

    public RequestFileList(String login, String path) {
        this.login = login;
        if (!path.isEmpty()) {
            fileList = new ArrayList<>();
            System.out.println(path);
            File dir = new File(path);

//            boolean usersCatalogExists = Files.exists(Paths.get(path));
//            if (!usersCatalogExists) {
//                //TODO хорошо бы где-нибудь залогировать для админа серьезную ошибку: отсутствует папка у авторизованного пользователя
//                fileList = new ArrayList<>();
//                return;
//            }

            File[] files = dir.listFiles();
            fileList = (ArrayList<String>) Arrays.stream(files)
                    .filter(o -> !o.mkdir())
                    .map((Function<File, String>) file -> file.getName())
                    .collect(Collectors.toList());

        }
    }

    public ArrayList<String> getFileList() {
        return (ArrayList<String>) fileList;
    }
}
