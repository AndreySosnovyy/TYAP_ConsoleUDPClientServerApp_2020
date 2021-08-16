package com.andreysosnovyy;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.NotActiveException;
import java.net.*;
import java.util.*;

public class NetUtils { // Server & Client utilities

    //  вернет null, если роль сервера свобода, иначе адрес сервера
    public static InetAddress broadcast(int port, int timeout) throws IOException {

        // широковещательный адрес
        InetAddress broadcastAddress = InetAddress.getByName("192.168.0.255");

        // открытие и настройка сокета
        DatagramSocket socket = new DatagramSocket(port);
        socket.setBroadcast(true);
        socket.setSoTimeout(timeout); // ждет ответа на приветствие timeout миллисекунд

        // поприветствовать всех (слово + рандомное int значение)
        int myValue = new Random().nextInt(); // рандомное число локалхоста
        String message = "Hello? " + myValue;
        byte[] buffer = message.getBytes();
        DatagramPacket outPacket = new DatagramPacket(buffer, buffer.length, broadcastAddress, port);
        socket.send(outPacket);
        System.out.println(">>> localhost: \"" + message + "\" --> broadcast");

        int newTimeout = timeout; // переменная для обновления таймаута после принятого сообщения

        while (true) { // пока не вылетит SocketTimeoutException ждать сообщения

            buffer = new byte[256];
            DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);

            long before = System.currentTimeMillis(); // время до начала ожидания (нужно для обновления таймаута)

            try { // попытка получить сообщение
                socket.receive(receivedPacket);
            } catch (SocketTimeoutException e) { // если за отведенное время сообщения не поступило
                socket.close();
                return null; // роль сервера свободна
            }

            // если ответ был получен, то проверить, является ли это сообщение от сервера + обработка коллизий
            String receivedMessage = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
            InetAddress opponent = receivedPacket.getAddress();
            System.out.println(">>> " + opponent + ": \"" + receivedMessage + "\"");

            if (receivedMessage.equals("I'm server!")) {
                socket.close();
                return opponent; // роль сервера уже занята -> стать клиентом
            } else if (receivedMessage.contains("Hello? ") && !opponent.toString().equals(getLocalHost())) {
                // если сообщение содержит приветствие и оно поступило не от локалхоста,
                // разрешить ситуацию, когда 2 хоста претендуют занять роль сервера (рандомное число)

                int opponentValue = Integer.parseInt(receivedMessage.substring(7)); // рандомное число оппонента

                if (myValue > opponentValue) { // если локалхост выиграл
                    // уведомить оппонента о поражении
                    buffer = "You lost!".getBytes();
                    outPacket = new DatagramPacket(buffer, buffer.length, opponent, port);
                    socket.send(outPacket);
                } else { // иначе выйти их цикла и ждать, пока сервер не разбудит
                    System.out.println(opponent + " has bigger value, localhost go sleep until notify");
                    break;
                }
            } else if (receivedMessage.equals("You lost!")) { // если сообщение уведомляет о поражении, сделать то же самое
                System.out.println(opponent + " has bigger value, localhost go sleep until notify");
                break;
            }

            // обновление таймаута
            long after = System.currentTimeMillis();
            if (newTimeout - (after - before) > 0) {
                newTimeout = (int) (newTimeout - (after - before));
                socket.setSoTimeout(newTimeout);
            } else {
                break;
            }
        }

        // ожидание пока локалхоста разбудит сервер + его приветствие, чтобы показать свой адрес
        buffer = new byte[256];
        DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
        socket.setSoTimeout(30000);

        socket.receive(receivedPacket);
        String receivedMessage = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
        System.out.println(">>> " + receivedPacket.getAddress() + ": \"" + receivedMessage + "\"");

        if (receivedMessage.equals("Wake up, I'm server!")) {
            InetAddress serverAddress = receivedPacket.getAddress();
            buffer = "Hello?".getBytes();
            outPacket = new DatagramPacket(buffer, buffer.length, serverAddress, port);
            socket.send(outPacket);
            System.out.println(">>> localhost: \"Hello?\" --> " + serverAddress);
            socket.close();
            return serverAddress;
        } else {
            socket.close();
            throw new IOException("Message from server was expected");
        }
    }


    // ждет клиентов и возвращает их в виде списка (заняв роль сервера)
    public static List<InetAddress> getClients(int port, int timeout) throws IOException {

        // открытие и настройка сокета
        DatagramSocket socket = new DatagramSocket(port);
        socket.setSoTimeout(timeout); // время, в течении которого, идет ожидание клиентов

        List<InetAddress> clients = new ArrayList<>();

        // уведомить тех, кто ждет ответа от сервера сервера
        InetAddress broadcastAddress = InetAddress.getByName("192.168.0.255");
        byte[] buffer = "Wake up, I'm server!".getBytes();
        DatagramPacket notifyPacket = new DatagramPacket(buffer, buffer.length, broadcastAddress, port);
        socket.send(notifyPacket);
        System.out.println(">>> localhost: \"Wake up, I'm server!\" --> broadcast");

        int newTimeout = timeout;

        // пока не все клиенты пришли
        while (true) {

            long before = System.currentTimeMillis();

            buffer = new byte[256];
            DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);

            // пытаться считать сообщение
            try {
                socket.receive(receivedPacket);
            } catch (SocketTimeoutException e) { // если время вышло, то вернуть список тех, кто успел прийти
                socket.close();
                return clients;
            }

            // если сообщение пришло, то ответить по адресу, что роль сервера занята и записать в список
            String receivedMessage = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
            System.out.println(">>> " + receivedPacket.getAddress() + ": \"" + receivedMessage + "\"");

            // если сообщение приветственное, то записать адрес в список клиентов и уведомить его, что роль сервера занята
            if (receivedMessage.contains("Hello?")) {
                clients.add(receivedPacket.getAddress());

                buffer = "I'm server!".getBytes();
                DatagramPacket answerPacket = new DatagramPacket(buffer, buffer.length, receivedPacket.getAddress(), port);
                socket.send(answerPacket);
                System.out.println(">>> localhost: \"I'm server!\" --> " + receivedPacket.getAddress());
            }

            // обновление таймаута
            long after = System.currentTimeMillis();
            if (newTimeout - (after - before) > 0) {
                newTimeout = (int) (newTimeout - (after - before));
                socket.setSoTimeout(newTimeout);
            } else {
                break;
            }
        }

        socket.close();
        return clients;
    }


    // возвращает ip4 адрес хоста
    public static String getLocalHost() throws IOException {
        Enumeration<?> e = NetworkInterface.getNetworkInterfaces();
        while (e.hasMoreElements()) {
            NetworkInterface networkInterface = (NetworkInterface) e.nextElement();
            Enumeration<?> ee = networkInterface.getInetAddresses();
            while (ee.hasMoreElements()) {
                InetAddress address = (InetAddress) ee.nextElement();
                if (address.toString().contains("192.168.0")) {
                    return address.toString();
                }
            }
        }
        throw new IOException("Unable to get local host address");
    }


    // отсылает изображение
    public static void sendImage(BufferedImage image, InetAddress address, int port) throws IOException {

        DatagramSocket socket = new DatagramSocket(port);

        byte[] buffer = AppUtils.imageToByteArray(image);
        DatagramPacket imagePacket = new DatagramPacket(buffer, buffer.length, address, port);
        socket.send(imagePacket);
        System.out.println(">>> localhost: **image part** --> " + address);

        socket.close();
    }


    // получает изображение
    public static BufferedImage receiveImage(int port, int timeout) throws IOException {

        DatagramSocket socket = new DatagramSocket(port);
        socket.setSoTimeout(timeout);

        byte[] buffer = new byte[0xffff];
        DatagramPacket receivedImagePacket = new DatagramPacket(buffer, buffer.length);

        try {
            socket.receive(receivedImagePacket);
        } catch (SocketTimeoutException e) {
            System.out.println("Image wasn't received");
            socket.close();
            throw new NotActiveException("no image");
        } finally {
            socket.close();
        }

        // если сервер послал команду остановить работу клиенту (только для клиентов)
        String receivedMessage = new String(receivedImagePacket.getData(), 0, receivedImagePacket.getLength());
        if (receivedMessage.equals("Stop!")) {
            System.out.println(">>> " + receivedImagePacket.getAddress() + ": \"Stop!\"");
            return null;
        } else { // получена часть изображения
            System.out.println(">>> " + receivedImagePacket.getAddress() + ": **image part**");
            return AppUtils.byteArrayToBufferedImage(buffer);
        }
    }


    // отправляет сообщение для всех клиентов
    public static void sendBroadcastMessage(String message, int port) throws IOException {
        DatagramSocket socket = new DatagramSocket(port);
        InetAddress broadcastAddress = InetAddress.getByName("192.168.0.255");

        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, port);
        socket.send(packet);
        System.out.println(">>> localhost: \"" + message + "\" --> broadcast");

        socket.close();
    }


    // отправляет сообщение для всех клиентов (задаются оба порта)
    public static void sendBroadcastMessage(String message, int myPort, int port) throws IOException {
        DatagramSocket socket = new DatagramSocket(myPort);
        InetAddress broadcastAddress = InetAddress.getByName("192.168.0.255");

        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, port);
        socket.send(packet);

        socket.close();
    }


    // ожидает сообщение
    public static void wait(String message, int port, int timeout) throws IOException {
        DatagramSocket socket = new DatagramSocket(port);
        socket.setSoTimeout(timeout);

        byte[] buffer = new byte[0xffff];
        DatagramPacket receivedImagePacket = new DatagramPacket(buffer, buffer.length);

        try {
            socket.receive(receivedImagePacket);
        } catch (SocketTimeoutException e) {
            throw new IOException("\"" + message + "\" wasn't received in " + timeout / 1000 + " seconds");
        }

        String receivedMessage = new String(receivedImagePacket.getData(), 0, receivedImagePacket.getLength());
        socket.close();
        if (receivedMessage.equals(message)) {
            System.out.println(">>> " + receivedImagePacket.getAddress() + ": \"" + receivedMessage + "\"");
        } else {

            throw new IOException("\"" + message + "\" were expected");
        }
    }


    // возвращает пары, содержащие адреса и части изображения, обработанные клиентами
    public static Map<InetAddress, BufferedImage> receiveImagePieces(int numberOfClients, int port, int timeout) throws IOException {

        DatagramSocket socket = new DatagramSocket(port);
        socket.setSoTimeout(timeout);

        Map<InetAddress, BufferedImage> map = new HashMap<>();
        List<DatagramPacket> packetsList = new ArrayList<>();

        for (int i = 0; i < numberOfClients; i++) {
            byte[] buffer = new byte[0xffff];
            DatagramPacket receivedImagePacket = new DatagramPacket(buffer, buffer.length);

            try {
                socket.receive(receivedImagePacket);
            } catch (SocketTimeoutException e) {
                for (DatagramPacket packet : packetsList) {
                    map.put(packet.getAddress(), AppUtils.byteArrayToBufferedImage(packet.getData()));
                }

                socket.close();
                return map;
            }

            packetsList.add(receivedImagePacket);
            System.out.println(">>> " + receivedImagePacket.getAddress() + ": **image part**");
        }

        for (DatagramPacket packet : packetsList) {
            map.put(packet.getAddress(), AppUtils.byteArrayToBufferedImage(packet.getData()));
        }

        socket.close();
        return map;
    }

    // пингует список клиентов
    public static List<InetAddress> pingClients(List<InetAddress> oldClients, int port, int timeout) throws IOException {
        List<InetAddress> clients = new ArrayList<>();
        DatagramSocket socket = new DatagramSocket(port);
        socket.setSoTimeout(timeout);

        for (InetAddress oldClient : oldClients) {
            byte[] buffer = "Ping".getBytes();
            DatagramPacket pingPacketOut = new DatagramPacket(buffer, buffer.length, oldClient, Main.PING_PORT);
            socket.send(pingPacketOut);

            buffer = new byte[256];
            DatagramPacket pingPacketIn = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(pingPacketIn);
            } catch (SocketTimeoutException e) {
                socket.send(pingPacketOut);
                try {
                    socket.receive(pingPacketIn);
                } catch (SocketTimeoutException e2) {
                    System.out.println("lost connection to " + oldClient);
                    continue;
                }
            }

            String receivedMessage = new String(pingPacketIn.getData(), 0, pingPacketIn.getLength());
            if (receivedMessage.equals("Alive")) {
                clients.add(oldClient);
            }
        }

        socket.close();
        return clients;
    }


    // отвечает на пинг-запрос
    public static boolean answerPing(int port, int timeout) throws IOException {
        DatagramSocket socket = new DatagramSocket(port);
        socket.setSoTimeout(timeout);

        byte[] buffer = new byte[256];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            socket.receive(packet);
        } catch (SocketTimeoutException e) {
            socket.close();
            return false;
        }

        String receivedMessage = new String(packet.getData(), 0, packet.getLength());
        if (receivedMessage.equals("Ping")) {
            buffer = "Alive".getBytes();
            InetAddress server = packet.getAddress();
            packet = new DatagramPacket(buffer, buffer.length, server, port);
            socket.send(packet);
            socket.close();
            return true;
        } else {
            socket.close();
            throw new IOException("Ping was expected");
        }
    }


    // после потери связи с сервером присоединяет обратно
    public static void connectToTheServer(int port, InetAddress serverAddress) throws IOException {
        DatagramSocket socket = new DatagramSocket(port);

        byte[] buffer = "Hello?".getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, port);
//        System.out.println(">>> localhost: \"Hello?\" --> " + serverAddress);
        System.out.println("...");
        socket.send(packet);
        socket.close();
    }


    // ждать нового клиента
    public static void waitForAClient(int port) throws IOException {
        DatagramSocket socket = new DatagramSocket(port);

        byte[] buffer = new byte[256];
        DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);

        socket.receive(receivedPacket);
        String receivedMessage = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
        System.out.println(receivedPacket.getAddress() + ": \"" + receivedMessage + "\"");
        if (receivedMessage.contains("Hello? ")) {

            buffer = "I'm server!".getBytes();
            DatagramPacket outPacket = new DatagramPacket(buffer, buffer.length, receivedPacket.getAddress(), port);
            socket.send(outPacket);

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            buffer = "Start".getBytes();
            outPacket = new DatagramPacket(buffer, buffer.length, receivedPacket.getAddress(), Main.WORK_PORT);
            socket.send(outPacket);

            if (!AppUtils.isClient(receivedPacket.getAddress())) {
                AppUtils.addClient(receivedPacket.getAddress());
                System.out.println("new client was found (" + receivedPacket.getAddress() + ")");
            }
        } else if (receivedMessage.equals("Hello?")) {
            if (!AppUtils.isClient(receivedPacket.getAddress())) {
                AppUtils.addClient(receivedPacket.getAddress());
                System.out.println("new client was found (" + receivedPacket.getAddress() + ")");
            }
        }
        socket.close();
    }


    // для отправки части изображения и сразу ожидания ее же обработанной
    public static BufferedImage sendReceiveImage(BufferedImage image, InetAddress client, int timeout) throws IOException {
        DatagramSocket socket = new DatagramSocket(Main.WORK_PORT);

        byte[] buffer = AppUtils.imageToByteArray(image);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, client, Main.WORK_PORT);
        socket.send(packet);
        socket.close();

        socket = new DatagramSocket(Main.WORK3_PORT);
        socket.setSoTimeout(timeout);

        buffer = new byte[0xffff];
        packet = new DatagramPacket(buffer, buffer.length);
        try {
            socket.receive(packet);
        } catch (SocketTimeoutException e) {
            return null;
        }

        socket.close();
        System.out.println(">>> " + packet.getAddress() + ": **image part**");
        return AppUtils.byteArrayToBufferedImage(buffer);
    }
}
