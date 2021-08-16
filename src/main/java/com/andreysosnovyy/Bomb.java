package com.andreysosnovyy;

import java.util.ArrayList;
import java.util.List;

public class Bomb extends Thread {
    private final List<Thread> threadList = new ArrayList<>();
    private final int timeout;

    public Bomb(Thread thread, int timeout) {
        threadList.add(thread);
        this.timeout = timeout;
    }

    public Bomb(Thread thread1, Thread thread2, Thread thread3, int timeout) {
        threadList.add(thread1);
        threadList.add(thread2);
        threadList.add(thread3);
        this.timeout = timeout;
    }

    @Override
    public void run() {
        System.out.println("Bomb has been planted (" + timeout/1000 + " sec)");
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException ignored) {
            System.out.println("Bomb has been defused");
            return;
        }
        System.out.println("BOOM");
        for (Thread thread : threadList) {
            thread.interrupt();
        }
        System.exit(0);
    }
}
