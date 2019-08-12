package com.gareeva.cloudStorage.client;

import com.gareeva.cloudStorage.common.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML
    HBox mainBox;

    @FXML
    ListView<String> localFilesList;

    @FXML
    ListView<String> remoteFilesList;

    //кнопки, доступные лишь авторизованным пользователям
    @FXML
    Button btnSendFile;

    @FXML
    Button btnUpdateRemoteFileList;

    @FXML
    Button btnDownloadFile;

    @FXML
    Button btnDeleteRemoteFile;

    @FXML
    Text txtLocalMessageStatus;

    @FXML
    Text txtRemoteMessageStatus;


    private Thread threadListener;
    private String login;
    private static final String LOCAL_STORAGE = "client/client_storage/";

    public void pressOnAuth(ActionEvent actionEvent) {
        //этот цикл на случай запуска клиента при незапущенном сервере
        while (true) {
            Network.start();
            if (Network.isConnectionEstablished()) {
                break;
            }
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Не удалось подключиться к серверу Cloud Storage. Повторить попытку?",
                    ButtonType.YES, ButtonType.CANCEL);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.CANCEL) {
                return;
            }
        }

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/auth.fxml"));
        Parent root = null;
        try {
            root = fxmlLoader.load();
            AuthController controller = fxmlLoader.getController();
            controller.setMainController(this);
            Stage authStage = new Stage();
            authStage.initModality(Modality.APPLICATION_MODAL);
            authStage.setTitle("Авторизация Cloud Storage");

            Scene scene = new Scene(root);
            authStage.setScene(scene);
            authStage.setResizable(false);
            authStage.setOnCloseRequest((WindowEvent event) -> {
                controller.closeResources();
            });
            authStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void changeWindowTitle() {
        Stage st = (Stage) mainBox.getScene().getWindow();
        st.setTitle("Cloud Storage. Пользователь " + login);
    }

    private enum Location {
        CLIENT,
        SERVER
    }

    public void setLogin(String login) {
        this.login = login;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //заблокируем кнопки, которые могут быть активны лишь при установленном соединении
        setButtonsDisable(true);
        refreshLocalFilesList();
    }

    private void setButtonsDisable(boolean disable) {
        btnDeleteRemoteFile.setDisable(disable);
        btnDownloadFile.setDisable(disable);
        btnSendFile.setDisable(disable);
        btnUpdateRemoteFileList.setDisable(disable);
    }

    public void createAndStartThreadForReadingMessages() {
        threadListener = new Thread(() -> {
            try {
                while (true) {
                    AbstractMessage am = Network.readObject();
                    if (am instanceof MessageFile) {
                        //здесь обрабатывается загрузка файла с сервера
                        MessageFile fm = (MessageFile) am;
                        createFileFromMessage(fm);

                    } else if (am instanceof RequestFileList) {
                        //здесь обрабатывается загрузка списка имен файлов, расположенных на сервере
                        RequestFileList flr = (RequestFileList) am;
                        refreshRemoteFilesListOnClient(flr.getFileList());

                    } else if (am instanceof ReportFileExists) {
                        //сервер вернул результат о наличии файла на сервере. Если файла на сервере нет,
                        //молча шлем файл на сервер. Если файл есть, спросим, требуется ли его перезаписать?
                        ReportFileExists rfe = (ReportFileExists) am;
                        String fileName = rfe.getFileName();
                        if (rfe.isFileExists()) {
                            Platform.runLater(() -> {
                                Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                                        "Перезаписать файл " + fileName + " в хранилище?",
                                        ButtonType.YES, ButtonType.CANCEL);
                                Optional result = alert.showAndWait();
                                if (result.get() == ButtonType.YES) {
                                    //шлем запрос на удаление файла на сервере
                                    Network.sendMsg(new RequestFileDelete(login, fileName));
                                    //теперь можно слать файл
                                    sendFileToServer(fileName);
                                }
                            });
                        } else {
                            sendFileToServer(fileName);
                        }

                    } else if (am instanceof RequestExit) {
                        //сервер вернул наш запрос на выход из системы, пора прерывать поток
                        break;

                    } else if (am instanceof ReportErrorFileOperation) {
                        //здесь обрабатывается сообщения об ошибках, произошедших на сервере при попытках удаления или скачивания файла
                        ReportErrorFileOperation reportError = (ReportErrorFileOperation) am;
                        showAlertFileNotExists(reportError.getFileName(), reportError.getOperation());
                    }

                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                Network.stop();
            }
        });
        threadListener.setDaemon(true);
        threadListener.start();

        sendRequestForUpdateRemoteFileList();
        setButtonsDisable(false);
    }

    private void createFileFromMessage(MessageFile fm) throws IOException {
        RandomAccessFile aFile = new RandomAccessFile(LOCAL_STORAGE + fm.getFilename(), "rw");
        FileChannel channel = aFile.getChannel();
        ByteBuffer buf = ByteBuffer.allocate(5 * 1024 * 1024);
        buf.clear();
        buf.put(fm.getData());
        buf.flip();
        channel.position(channel.size());
        while (buf.hasRemaining()) {
            txtLocalMessageStatus.setText("Загружается файл " + fm.getFilename() + " часть " + fm.getNumberOfPackage() + " из " + fm.getTotalPackages() + "...");
            channel.write(buf);
        }
        if (fm.isLastPackage()) {
            txtLocalMessageStatus.setText("");
            refreshLocalFilesList();
        }
        channel.close();
    }

    private void sendFileToServer(String fileName) {
        String selectedFile = LOCAL_STORAGE + fileName;
        try (FileInputStream in = new FileInputStream(selectedFile)) {

            int byteBuffSize = 4 * 1024 * 1024;
            int sizeOfFile = in.available();
            int byteBuffLength = (sizeOfFile - byteBuffSize < 0) ? sizeOfFile : byteBuffSize;
            byte[] byteBuff = new byte[byteBuffLength];
            int number = 0;
            int totalPackages = 1;
            if (sizeOfFile > 0) {
                totalPackages = sizeOfFile / byteBuffLength + 1;
            }
            int delta = sizeOfFile;
            Path path = Paths.get(selectedFile);
            while (delta > byteBuffLength) {
                in.read(byteBuff);
                number++;
                txtLocalMessageStatus.setText("Отправляется файл " + selectedFile + " часть " + number + " из " + totalPackages + "...");
                Network.sendMsg(new MessageFile(login, path, byteBuff, number, totalPackages));
                delta -= byteBuffLength;
            }
            //отправим остаток файла в маленькой посылке
            if (delta > 0 || sizeOfFile == 0) {
                byte[] tail = new byte[delta];
                in.read(tail);
                txtLocalMessageStatus.setText("Отправляется файл " + selectedFile + " часть " + number + " из " + totalPackages + "...");
                Network.sendMsg(new MessageFile(login, path, tail, number, totalPackages));
            }
            txtLocalMessageStatus.setText("");

            sendRequestForUpdateRemoteFileList();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void sendRequestExit() {
        if (Network.isConnectionEstablished()) {
            Network.sendMsg(new RequestExit());
        }
    }

    private void sendRequestForUpdateRemoteFileList() {
        Network.sendMsg(new RequestFileList(login, ""));
    }


    public void pressOnDownloadBtn(ActionEvent actionEvent) {
        String selectedFile = getRemoteSelectedFileName();
        if (selectedFile.isEmpty()) {
            showAlertNoFileSelected("Не выбран файл для загрузки.");
            return;
        }
        boolean fileExists = Files.exists(Paths.get(LOCAL_STORAGE + selectedFile));
        if (fileExists) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    "Перезаписать файл " + selectedFile,
                    ButtonType.YES, ButtonType.CANCEL);
            Optional result = alert.showAndWait();
            if (result.get() == ButtonType.CANCEL) {
                return;
            }
            //удаляем файл локально и начинаем скачивание
            try {
                Files.deleteIfExists(Paths.get(LOCAL_STORAGE + selectedFile));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Network.sendMsg(new RequestFile(login, selectedFile));
    }

    public void pressOnSendBtn(ActionEvent actionEvent) {
        String selectedFile = getLocalSelectedFileName();
        if (selectedFile.isEmpty()) {
            showAlertNoFileSelected("Не выбран файл для отправки.");
            return;
        }
        Path path = Paths.get(selectedFile);
        if (Files.notExists(path)) {
            //файл не может быть отправлен в хранилище, т.к. он не существует. Сообщим пользователю
            showAlertFileNotExists(selectedFile, FileTypeOperation.SEND);
            return;
        }

        //нужно спросить у сервера, вдруг файл с таким именем на сервере уже есть.
        //если есть, спросим, затереть или отменить пересылку.
        //Если ничего не спрашивать, а просто слать, размер файла на сервере будет разрастаться.
        Network.sendMsg(new RequestFileExists(login, path.getFileName().toString()));

    }

    public void pressOnUpdateLocalFilesList(ActionEvent actionEvent) {
        refreshLocalFilesList();
    }

    public void pressOnUpdateRemoteFilesList(ActionEvent actionEvent) {
        sendRequestForUpdateRemoteFileList();
    }

    public void pressOnDeleteLocalFileBtn(ActionEvent actionEvent) {
        String fileName = getLocalSelectedFileName();
        if (fileName.isEmpty()) {
            showAlertNoFileSelected("Не выбран удаляемый файл.");
            return;
        }
        if (!showAlertAndGetConfirmationOfDeleteFile(fileName, Location.CLIENT)) {
            return;
        }
        Path path = Paths.get(fileName);
        try {
            boolean fileDeleted = Files.deleteIfExists(path);
            if (fileDeleted) {
                refreshLocalFilesList();
            } else {
                //выводим предупреждение
                showAlertFileNotExists(fileName, FileTypeOperation.DELETE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pressOnDeleteRemoteFileBtn(ActionEvent actionEvent) {
        String fileName = getRemoteSelectedFileName();
        if (fileName.isEmpty()) {
            showAlertNoFileSelected("Не выбран файл для удаления из хранилища.");
            return;
        }
        if (!showAlertAndGetConfirmationOfDeleteFile(fileName, Location.SERVER)) {
            return;
        }
        sendRequestForDeleteRemoteFile(fileName);
    }

    private void sendRequestForDeleteRemoteFile(String fileName) {
        Network.sendMsg(new RequestFileDelete(login, fileName));
    }

    private void showAlertNoFileSelected(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.CLOSE);
        alert.show();
    }

    private void showAlertFileNotExists(String fileName, FileTypeOperation operation) {
        updateUI(() -> {
            String text;
            switch (operation) {
                case SEND: {
                    text = "отправить";
                    break;
                }
                case DELETE: {
                    text = "удалить";
                    break;
                }
                default:
                    throw new RuntimeException("Unknown FileTypeOperation");
            }
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Не удалось " + text + " файл " + fileName +
                            "\nОбновите список файлов, возможно, файл с указанным именем не существует.",
                    ButtonType.CLOSE);
            alert.show();
        });
    }

    private boolean showAlertAndGetConfirmationOfDeleteFile(String fileName, Location location) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Вы действительно хотите удалить файл " + fileName
                        + (location == Location.CLIENT ? " локально?" : " из хранилища?"), ButtonType.YES,
                ButtonType.NO);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.NO) {
            return false;
        }
        return true;
    }

    private String getRemoteSelectedFileName() {
        String selectedFile = remoteFilesList.getSelectionModel().getSelectedItem();
        if (selectedFile != null) {
            return selectedFile;
        }
        return "";
    }

    private String getLocalSelectedFileName() {
        String selectedFile = localFilesList.getSelectionModel().getSelectedItem();
        if (selectedFile != null) {
            return LOCAL_STORAGE + selectedFile;
        }
        return "";
    }

    public void refreshLocalFilesList() {
        updateUI(() -> {
            try {
                localFilesList.getItems().clear();
                Files.list(Paths.get(LOCAL_STORAGE)).map(p -> p.getFileName().toString()).forEach(o -> localFilesList.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void refreshRemoteFilesListOnClient(ArrayList<String> fileList) {
        updateUI(() -> {
            remoteFilesList.getItems().clear();
            remoteFilesList.getItems().addAll(fileList);
        });
    }

    public static void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }
}

