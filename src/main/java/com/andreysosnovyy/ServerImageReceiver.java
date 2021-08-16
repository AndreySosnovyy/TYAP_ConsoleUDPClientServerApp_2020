package com.andreysosnovyy;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

public class ServerImageReceiver extends Thread {

    private Object getMonitor() {
        return Server.MONITOR;
    }

    private List<InetAddress> getClients() {
        return Server.clients;
    }

    @Override
    public void run() {
        super.run();

        while (!Thread.currentThread().isInterrupted()) {
            // ждать, пока сервер не разбудит
            synchronized (getMonitor()) {
                try {
                    getMonitor().wait();
                } catch (InterruptedException e) {
                    break;
                }
            }

            // получать обработанные части изображения от клиентов
            try {
                Server.imageMap = NetUtils.receiveImagePieces(getClients().size(), Main.WORK2_PORT, 20_000);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // будить сервер, чтобы он делал свою работу дальше
            synchronized (getMonitor()) {
                getMonitor().notify();
            }
        }
    }
}
