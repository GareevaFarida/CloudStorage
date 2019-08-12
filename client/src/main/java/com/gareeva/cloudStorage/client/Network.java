package com.gareeva.cloudStorage.client;

import com.gareeva.cloudStorage.common.AbstractMessage;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.IOException;
import java.net.Socket;

class Network {
    private static Socket socket;
    private static ObjectEncoderOutputStream out;
    private static ObjectDecoderInputStream in;
    private static volatile boolean connectionEstablished;

    static void start() {
        try {
            socket = new Socket("localhost", 8189);
            out = new ObjectEncoderOutputStream(socket.getOutputStream());
            in = new ObjectDecoderInputStream(socket.getInputStream(), 50 * 1024 * 1024);
            connectionEstablished = true;
        } catch (IOException e) {
            connectionEstablished = false;
           // e.printStackTrace();
            System.out.println("Сервер не запущен.");
        }
    }

    static boolean isConnectionEstablished() {
        return connectionEstablished;
    }

    static void stop() {
        connectionEstablished = false;
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static boolean sendMsg(AbstractMessage msg) {
        try {
            out.writeObject(msg);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    static AbstractMessage readObject() throws ClassNotFoundException, IOException {
        if (!isConnectionEstablished()) {
            return null;
        }
        Object obj = in.readObject();
        return (AbstractMessage) obj;
    }
}
