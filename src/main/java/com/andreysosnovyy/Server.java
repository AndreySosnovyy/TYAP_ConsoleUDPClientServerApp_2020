package com.andreysosnovyy;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server {

    public final static Object MONITOR = new Object(); // для синхронизации получения частей изображения
    public static List<InetAddress> clients;
    public static Map<InetAddress, BufferedImage> imageMap;

    public void run() throws IOException, InterruptedException {

        // запускать поток пингер-отправитель
        Thread pingTransmitter = new PingTransmitter(Main.PING_PORT);
        pingTransmitter.start();

        // запускать поток коннектор
        Thread connector = new Connector(Main.CONNECT_PORT);
        connector.start();

        // запускать поток ресивер частей изображения
        Thread serverImageReceiver = new ServerImageReceiver();
        serverImageReceiver.setPriority(Thread.MAX_PRIORITY);
        serverImageReceiver.start();

        NetUtils.sendBroadcastMessage("Start", Main.WORK_PORT);

        Bomb bomb = new Bomb(pingTransmitter, connector, serverImageReceiver, 30_000);;

        int iteration = 0;
        while (iteration != 30) {

            // получить актуальный список клиентов
            clients = AppUtils.getClients(); // фиксирует список клиентов на текущую итерацию

            if (clients.size() == 0) {
                // взрывать сервер, если клиентов нет определенное время
                if (!bomb.isAlive()) {
                    bomb.start();
                }
                continue; // если нет клиентов, просто переходить на следующую итерацию
            } else {
                if (bomb.isAlive()) {
                    bomb.interrupt(); // остановить бомбу
                    bomb = new Bomb(pingTransmitter, connector, serverImageReceiver, 30_000); // перезарядить бомбу
                }
                // если есть переподключенные клиенты, им надо проснуться (через свой пингер)
                NetUtils.sendBroadcastMessage("Ping", Main.WORK_PORT, Main.PING_PORT);
            }

            BufferedImage image = ImageIO.read(AppUtils.file); // оригинал изображения

            int width = image.getWidth(); // ширина полученного изображения
            int widthOfSegment = width / (clients.size()); // ширина сегмента
            Map<InetAddress, Integer> addressMap = new HashMap<>();

            synchronized (MONITOR) {
                MONITOR.notify(); // разбудить поток, который будет принимать готовые части изображения
            }

            // отправка частей изображения клиентам
            for (int clientIndex = 0; clientIndex < clients.size(); clientIndex++) {

                // разделение картинки на части
                BufferedImage imagePart = image.getSubimage(clientIndex * widthOfSegment, 0,
                        widthOfSegment, image.getHeight());

                // сервер раздает части клиентам
                NetUtils.sendImage(imagePart, clients.get(clientIndex), Main.WORK_PORT);

                // пары адрес:номер части изображения
                addressMap.put(clients.get(clientIndex), clientIndex);
            }

            synchronized (MONITOR) {
                MONITOR.wait(); // ждать, пока ресивер не получит все части изображения и не разбудит
            }

            // могут быть получены не все части изображения
            while (addressMap.size() != imageMap.size()) {
                if (AppUtils.getClients().size() != 0) {
                    for (InetAddress address : clients) {
                        if (!imageMap.containsKey(address)) {
                            int clientIndex = addressMap.get(address);
                            BufferedImage partToEdit  = image.getSubimage(
                                    clientIndex * widthOfSegment, 0,
                                    widthOfSegment, image.getHeight());

                            // отправить другому клиенту
                            BufferedImage editedImage = NetUtils.sendReceiveImage(partToEdit, AppUtils.getClients().get(0), 10_000);
                            if (editedImage != null) {
                                imageMap.put(address, editedImage);
                            }
                        }
                    }
                }
            }

            // сервер собирает цельное изображение из частей и выводит его
            BufferedImage fullImage = AppUtils.combineParts(clients, imageMap, addressMap);
            AppUtils.showImage(fullImage, iteration);

            iteration++;

            Thread.sleep(1000);
        }

        // сказать клиентам завершить работу
        NetUtils.sendBroadcastMessage("Stop!", Main.WORK_PORT);
        serverImageReceiver.interrupt();
        pingTransmitter.interrupt();
        connector.interrupt();
        System.out.println("All work is done");
    }
}
