package com.andreysosnovyy;

import java.io.IOException;

public class PingTransmitter extends Thread {

    private final int port;

    public PingTransmitter(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        super.run();

        while (!Thread.currentThread().isInterrupted()) {
            try {
                AppUtils.setClients(port);
                Thread.sleep(100);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
