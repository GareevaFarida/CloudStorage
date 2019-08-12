package com.gareeva.cloudStorage.client;

import com.gareeva.cloudStorage.common.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class AuthController implements Initializable {
    @FXML
    VBox authVBox;

    @FXML
    Text textAuthError;

    @FXML
    TextField tfLogin;

    @FXML
    TextField tfPassword;

    private MainController mainController;
    private Thread authTread;
    private boolean authSuccess = false;

    private String login;
    private String password;

    void setMainController(MainController mainController) {
        this.mainController = mainController;
    }


    public void pressOnAuth(ActionEvent actionEvent) {
        login = tfLogin.getText();
        if (login.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Логин не указан!", ButtonType.CLOSE);
            alert.show();
            return;
        }
        password = tfPassword.getText();
        Network.sendMsg(new RequestAuth(login, password));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        textAuthError.setVisible(false);

        authTread = new Thread(() -> {
            //здесь обрабатываается ответ сервера на аутентификацию/авторизацию
            try {
                while (true) {
                    AbstractMessage msg = Network.readObject();
                    if (msg instanceof ReportAuth) {
                        ReportAuth report = (ReportAuth) msg;
                        if (report.isSuccess()) {
                            //ура! аутентификация пройдена!
                            //закрываем окно аутентификации, передаем логин
                            showMainWindowForAuthUsers();
                            break;
                        } else {
                            //вывод на форме сообщения об ошибке аутентификации
                            Platform.runLater(() -> showTextAuthError("Ошибка авторизации. Неверный логин и/или пароль."));
                        }
                    } else if (msg instanceof ReportRegistration) {
                        ReportRegistration rr = (ReportRegistration) msg;
                        if (rr.isSuccess()) {
                            //ура! регистрация прошла успешно!
                            showMainWindowForAuthUsers();
                            break;
                        } else {
                            //вывод на форме сообщения о неудачной регистрации
                            Platform.runLater(() -> showTextAuthError("Не удалось зарегистрировать пользователя."));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        authTread.setDaemon(true);
        authTread.start();
    }

    private void showMainWindowForAuthUsers() {
        mainController.setLogin(login);
        Platform.runLater(() -> mainController.changeWindowTitle());
        mainController.createAndStartThreadForReadingMessages();
        Stage authStage = (Stage) authVBox.getScene().getWindow();
        Platform.runLater(() -> authStage.close());
    }

    public void closeResources() {
        if (authTread.isAlive()) {
            authTread.interrupt();
        }
    }

    private void showTextAuthError(String textError) {
        textAuthError.setText(textError);
        textAuthError.setVisible(true);
    }


    public void pressOnKeyPressed(KeyEvent keyEvent) {
        textAuthError.setVisible(false);
    }

    public void pressOnRegistration(ActionEvent actionEvent) {
        login = tfLogin.getText();
        if (login.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Логин не указан!", ButtonType.CLOSE);
            alert.show();
            return;
        }
        password = tfPassword.getText();
        Network.sendMsg(new RequestRegistration(login, password));
    }
}
