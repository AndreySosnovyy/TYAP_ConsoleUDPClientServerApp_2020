package com.andreysosnovyy;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.NotActiveException;
import java.net.InetAddress;

public class Client {

    private final InetAddress serverAddress;

    public static boolean isAlive = true;
    public static final Object ALIVE_MONITOR = new Object();

    public static void setAlive(boolean value) {
        synchronized (ALIVE_MONITOR) {
            isAlive = value;
        }
    }

    public static boolean getAlive() {
        synchronized (ALIVE_MONITOR) {
            return isAlive;
        }
    }

    public Client(InetAddress serverAddress) {
        this.serverAddress = serverAddress;
    }


    public void run() throws IOException, InterruptedException {

        NetUtils.wait("Start", Main.WORK_PORT, 60_000);

        // запускать поток пингер-получатель
        Thread pingReceiver = new PingReceiver(serverAddress, Main.PING_PORT, 1_000);
        pingReceiver.start();

        Bomb bomb = new Bomb(pingReceiver, 10_000);

        while (true) {
            if (getAlive()) { // клиент активен и работает

                if (bomb.isAlive()) {
                    bomb.interrupt(); // остановить бомбу
                    bomb = new Bomb(pingReceiver, 10_000); // перезарядить бомбу
                }

                // получение части изображения от сервера
                BufferedImage image;
                try {
                    image = NetUtils.receiveImage(Main.WORK_PORT, 10_000);
                } catch (NotActiveException ignored) {
                    continue; // изображение не было получено
                }

                if (image == null) { // сервер прислал стоп-сообщение
                    System.out.println("localhost has done all client work");
                    break;
                } else {
                    // обработка изображения
                    BufferedImage editedImage = AppUtils.editImage(image);

                    // отправка обработанного изображения обратно серверу
                    NetUtils.sendImage(editedImage, serverAddress, Main.WORK2_PORT);
                    NetUtils.sendImage(editedImage, serverAddress, Main.WORK3_PORT);
                }
            } else {
                bomb.start();
                synchronized (ALIVE_MONITOR) {
                    ALIVE_MONITOR.wait();
                }
            }
        }

        pingReceiver.interrupt();
        System.exit(0);
    }



}
