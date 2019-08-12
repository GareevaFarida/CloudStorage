package com.gareeva.cloudStorage.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;

public class MainClient extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/main.fxml"));
        Parent root = fxmlLoader.load();
        primaryStage.setTitle("Cloud Storage. Пользователь не авторизован.");
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        MainController mainController = fxmlLoader.getController();
        primaryStage.setOnCloseRequest((WindowEvent event) -> mainController.sendRequestExit());
        primaryStage.show();
    }

    public static void main(String[] args) throws IOException {
        launch(args);
    }
}