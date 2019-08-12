package com.gareeva.cloudStorage.server;

import com.gareeva.cloudStorage.common.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof RequestFile) {
                //здесь обрабатываается запрос пользователя на скачивание файла с сервера на клиент
                RequestFile fr = (RequestFile) msg;
                String fileName = getFileName(fr.getLogin(), fr.getFilename());
                if (Files.exists(Paths.get(fileName))) {
                    sendFileToClient(ctx, fileName, fr.getLogin());
                    // MessageFile fm = new MessageFile(fr.getLogin(), Paths.get(fileName));
                    // ctx.writeAndFlush(fm);
                } else {
                    //сообщим пользователю, что файл не существует
                    ctx.writeAndFlush(new ReportErrorFileOperation(fr.getFilename(), FileTypeOperation.SEND));
                }
                return;
            }
            if (msg instanceof MessageFile) {
                // здесь обрабатывается получение файла
                MessageFile fm = (MessageFile) msg;
                System.out.println("Сообщение получено в MainHandler");
                ctx.fireChannelRead(msg);
                return;
            }
            if (msg instanceof RequestFileList) {
                //здесь обрабатывается запрос клиента на список файлов на сервере
                String login = ((RequestFileList) msg).getLogin();
                RequestFileList flr = new RequestFileList(login, Constants.SERVER_STORAGE + login);
                ctx.writeAndFlush(flr);
                return;
            }
            if (msg instanceof RequestFileExists) {
                RequestFileExists rfe = (RequestFileExists) msg;
                String login = (rfe).getLogin();
                String filename = rfe.getFileName();
                Path path = Paths.get(getFileName(login, filename));
                boolean fileExistsOnServer = Files.exists(path);
                ctx.writeAndFlush(new ReportFileExists(filename, fileExistsOnServer));
                return;
            }
            if (msg instanceof RequestExit) {
                ctx.writeAndFlush(msg);
                return;
            }
            if (msg instanceof RequestFileDelete) {
                //здесь обрабатывается запрос клиента на удаление файла на сервере
                RequestFileDelete fdr = (RequestFileDelete) msg;
                String login = fdr.getLogin();
                String fileName = fdr.getFileName();
                Path path = Paths.get(getFileName(login, fileName));
                boolean successOfDelete = Files.deleteIfExists(path);
                if (successOfDelete) {
                    //успешно удалили файл на сервере, нужно обновить список "облачных" файлов у пользователя
                    RequestFileList flr = new RequestFileList(login, getFileName(login, ""));
                    ctx.writeAndFlush(flr);
                } else {
                    //файл удалить не удалось, сообщим об этом пользователю
                    ctx.writeAndFlush(new ReportErrorFileOperation(fileName, FileTypeOperation.DELETE));
                }
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    private void sendFileToClient(ChannelHandlerContext ctx, String fileName, String login) {
        try (FileInputStream in = new FileInputStream(fileName)) {

            int byteBuffSize = 5 * 1024 * 1024;
            int sizeOfFile = in.available();
            int byteBuffLength = (sizeOfFile - byteBuffSize < 0) ? sizeOfFile : byteBuffSize;
            byte[] byteBuff = new byte[byteBuffLength];
            int number = 0;
            int delta = sizeOfFile;
            int totalPackages = 1;
            if (sizeOfFile > 0) {
                totalPackages = sizeOfFile / byteBuffLength + 1;
            }
            Path path = Paths.get(fileName);
            while (delta > byteBuffLength) {
                in.read(byteBuff);
                number++;
                ctx.writeAndFlush(new MessageFile(login, Paths.get(fileName), byteBuff, number, totalPackages));
                delta -= byteBuffLength;
            }
            //отправим остаток файла в маленькой посылке
            if (delta > 0 || sizeOfFile == 0) {
                byte[] tail = new byte[delta];
                in.read(tail);
                MessageFile message = new MessageFile(login, path, tail, totalPackages, totalPackages);
                ctx.writeAndFlush(message);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getFileName(String login, String fileName) {
        return Constants.SERVER_STORAGE + login + "/" + fileName;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
