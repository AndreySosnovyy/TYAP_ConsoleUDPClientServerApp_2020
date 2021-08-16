package com.andreysosnovyy;

import java.io.IOException;
import java.net.InetAddress;

public class PingReceiver extends Thread {

    private final InetAddress serverAddress;
    private final int port;
    private final int timeout;

    public PingReceiver(InetAddress serverAddress, int port, int timeout) {
        this.serverAddress = serverAddress;
        this.port = port;
        this.timeout = timeout;
    }

    @Override
    public void run() {
        super.run();

        boolean messageFlag = false;

        while (!Thread.currentThread().isInterrupted()) {
            boolean isAlive = false;
            try {
                isAlive = NetUtils.answerPing(port, timeout);
                Client.setAlive(isAlive);
                if (messageFlag && isAlive) messageFlag = false; // сброс флага
                if (isAlive) { // разбудить клиента, если соединение с сервером
                    synchronized (Client.ALIVE_MONITOR) {
                        Client.ALIVE_MONITOR.notify();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (!isAlive) { // нет соединения с сервером
                if (!messageFlag) {
                    System.out.println("reconnecting to the server...");
                    messageFlag = true;
                }
                try {
                    NetUtils.connectToTheServer(Main.CONNECT_PORT, serverAddress);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
