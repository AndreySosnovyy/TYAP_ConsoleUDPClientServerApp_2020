package com.andreysosnovyy;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class AppUtils {

    public static File file = null; // путь к изображению

    // получение пути к изображению
    public static void getImage() throws IOException {
        Path path = Paths.get("C:\\images");
        JFileChooser fileChooser = new JFileChooser(path.toString());
        fileChooser.setDialogTitle("Choose image");
        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            file = fileChooser.getSelectedFile(); // успешное чтение
        } else { // иначе выйти из программы
            throw new IOException("No file chosen exception");
        }
    }


    // BufferedImage --> byte[]
    public static byte[] imageToByteArray(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }


    // byte[] --> BufferedImage
    public static BufferedImage byteArrayToBufferedImage(byte[] bytes) throws IOException {
        InputStream inputStream = new ByteArrayInputStream(bytes);
        return ImageIO.read(inputStream);
    }


    // обработать всё полученное изображение
    public static BufferedImage editImage(BufferedImage image) {
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                image.setRGB(i, j, editPixel(image.getRGB(i, j)));
            }
        }
        return image;
    }


    // фильтр
    private static int editPixel(int pixel) {
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;
        return (blue << 16) | (red << 8) | green;
    }


    public static void showImage(BufferedImage image, int number) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(image.getWidth() + 100, image.getHeight() + 100);
        frame.setTitle(String.valueOf(number));
        frame.setLocation(10 + number * 5, 10 + number * 10);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(image, 0, 0, null);
            }
        };

        frame.add(panel);
        frame.setVisible(true);
    }


    // объединяет изображение из частей
    public static BufferedImage combineParts(List<InetAddress> clients, Map<InetAddress, BufferedImage> imageMap,
                                             Map<InetAddress, Integer> addressMap) {

        int partWidth = imageMap.get(clients.get(0)).getWidth();
        int height = imageMap.get(clients.get(0)).getHeight();
        BufferedImage image = new BufferedImage(partWidth * imageMap.size(), height, 5);

        for (int i = 0; i < imageMap.size(); i++) {
            for (int j = 0; j < partWidth; j++) {
                for (int k = 0; k < height; k++) {
                    image.setRGB(j + partWidth * addressMap.get(clients.get(i)), k, imageMap.get(clients.get(i)).getRGB(j, k));
                }
            }
        }

        return image;
    }


    // возвращает актуальный список клиентов
    public static List<InetAddress> getClients() {
        synchronized (Main.MONITOR) {
            return Main.clients;
        }
    }


    // создает актуальный список клиентов в Main
    public static void setClients(int port) throws IOException {
        synchronized (Main.MONITOR) {
            Main.clients = NetUtils.pingClients(Main.clients, port, 300);
        }
    }


    // добавляет одного клиента в список клиентов
    public static void addClient(InetAddress newClient) {
        synchronized (Main.MONITOR) {
            if (!Main.clients.contains(newClient)) {
                Main.clients.add(newClient);
            }
        }
    }


    // проверка хоста на роль клиента
    public static boolean isClient(InetAddress address) {
        synchronized (Main.MONITOR) {
            return Main.clients.contains(address);
        }
    }
}
