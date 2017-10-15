package ru.nsu.ccfit.skokova.treechat.node;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

public class TreeNode {
    private static final int BUF_SIZE = 2048;
    private static final int QUEUE_SIZE = 100;
    private ArrayList<DatagramSocket> childrenSockets = new ArrayList<>();
    private DatagramSocket socket;
    private String name;
    private int percentageLoss;
    private int port;
    private String parentAddress;
    private int parentPort;
    private Thread inThread;
    private Thread outThread;
    private ArrayBlockingQueue<Object> messagesToSend = new ArrayBlockingQueue<>(QUEUE_SIZE);

    public TreeNode(String name, int percentageLoss, int port) {
        this.name = name;
        this.percentageLoss = percentageLoss;
        this.port = port;
    }

    public TreeNode(String name, int percentageLoss, int port, String parentAddress, int parentPort) {
        this(name, percentageLoss, port);
        this.parentAddress = parentAddress;
        this.parentPort = parentPort;
    }

    public void start() {
        try {
            socket = new DatagramSocket(port);
            inThread = new Thread(new Receiver());
            outThread = new Thread(new Sender());
            inThread.start();
            outThread.start();
        } catch (SocketException e) {
            System.out.println(e.getMessage());
        }
    }

    class Sender implements Runnable {

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    Object message = messagesToSend.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class Receiver implements Runnable {

        @Override
        public void run() {
            byte[] buffer = new byte[BUF_SIZE];
            DatagramPacket datagramPacket = new DatagramPacket(buffer, BUF_SIZE);
            while (!Thread.interrupted()) {
                try {
                    socket.receive(datagramPacket);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }
}
