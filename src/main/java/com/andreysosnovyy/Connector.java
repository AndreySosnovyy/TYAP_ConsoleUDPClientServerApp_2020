package com.andreysosnovyy;

import java.io.IOException;

public class Connector extends Thread {

    private final int port;

    public Connector(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        super.run();

        while (!Thread.currentThread().isInterrupted()) {
            try {
                NetUtils.waitForAClient(port);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
