package com.andreysosnovyy;

import java.net.InetAddress;
import java.util.List;

public class Main {

    public static final int CONNECT_PORT = 49100; // используется для подключения клиентов
    public static final int WORK_PORT = 49101; // используется для пересылки пакетов с полезной нагрузкой (основной)
    public static final int WORK2_PORT = 49102; // используется для пересылки пакетов с полезной нагрузкой (дополнительный)
    public static final int WORK3_PORT = 49103; // используется для пересылки пакетов с полезной нагрузкой (дополнительный)
    public static final int PING_PORT = 49104; // используется для пингов

    public static List<InetAddress> clients;
    public static final Object MONITOR = new Object(); // для общего списка клиентов

    public static void main(String[] args) throws Exception {

        // получить адрес сервера, либо, если роль свободна, занять ее
        InetAddress serverAddress = NetUtils.broadcast(CONNECT_PORT, 1000);
        if (serverAddress == null) { // если роль сервера свободна, то занять и ожидать подключения клиентов
            System.out.println("Server role is taken by localhost (" + InetAddress.getLocalHost().getHostName() + NetUtils.getLocalHost() + ")");

            // ждать клиентов
            clients = NetUtils.getClients(CONNECT_PORT, 10_000);

            // вывод информации о полученном списке клиентов:
            // в случае неудачи
            if (clients.size() == 0) {
                System.out.println("No clients were found");
                System.exit(1);
            }

            // в случае, когда клиенты найдены
            System.out.println("Clients list :");
            for (InetAddress client : clients) {
                System.out.println("\t" + client.getHostName() + '/' + client.getHostAddress());
            }
            System.out.println("----------------------------------------------------------------------");

            AppUtils.getImage(); // находим изображение

            while (AppUtils.file == null) {
                Thread.sleep(1);
            }

            Server server = new Server();
            server.run();

        } else { // если текущий процесс является клиентом
            System.out.println("localhost is a client (my address is " + InetAddress.getLocalHost().getHostName() + NetUtils.getLocalHost() + "; " +
                    "server address is " + serverAddress + ")");

            Client client = new Client(serverAddress);
            client.run();
        }
    }
}
