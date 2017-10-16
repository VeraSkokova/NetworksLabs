package ru.nsu.ccfit.skokova.treechat.node;

import ru.nsu.ccfit.skokova.treechat.messages.JoinMessage;
import ru.nsu.ccfit.skokova.treechat.messages.Message;
import ru.nsu.ccfit.skokova.treechat.serialization.Serializer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

public class TreeNode {
    private static final int BUF_SIZE = 2048;
    private static final int QUEUE_SIZE = 100;
    private ArrayList<InetSocketAddress> neighbourAddresses = new ArrayList<>();
    private DatagramSocket socket;
    private String name;
    private int percentageLoss;
    private int port;
    private String parentAddress;
    private int parentPort;
    private boolean isRoot;
    private Thread inThread;
    private Thread outThread;
    private ArrayBlockingQueue<Message> messagesToSend = new ArrayBlockingQueue<>(QUEUE_SIZE);

    public TreeNode(String name, int percentageLoss, int port) {
        this.name = name;
        this.percentageLoss = percentageLoss;
        this.port = port;
        this.isRoot = true;
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
            joinChat();
        } catch (SocketException e) {
            System.out.println(e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
            Thread.currentThread().interrupt();
        }
    }

    public ArrayList<InetSocketAddress> getNeighbourAddresses() {
        return neighbourAddresses;
    }

    public ArrayBlockingQueue<Message> getMessagesToSend() {
        return messagesToSend;
    }

    public String getName() {
        return name;
    }

    private void joinChat() throws SocketException, InterruptedException {
        if (!isRoot) {
            neighbourAddresses.add(new InetSocketAddress(parentAddress, parentPort));
            messagesToSend.put(new JoinMessage());
        }
    }

    class Sender implements Runnable {

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    Message message = messagesToSend.take();
                    byte[] messageBytes = Serializer.serialize(message);
                    for (InetSocketAddress inetSocketAddress : neighbourAddresses) {
                        if (!isAuthor()) {
                            DatagramPacket datagramPacket = new DatagramPacket(messageBytes, messageBytes.length, inetSocketAddress.getAddress(), inetSocketAddress.getPort());
                            socket.send(datagramPacket);
                        }
                    }
                } catch (InterruptedException | IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }

        private boolean isAuthor() {
            return false;
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
                    ByteBuffer byteBuffer = ByteBuffer.wrap(Arrays.copyOfRange(datagramPacket.getData(), 0, 3));
                    //int messageType = byteBuffer.getInt();
                    //Message message = (Message) MessageCreator.getClassByIndex(messageType).newInstance();
                    Message message = Serializer.deserialize(datagramPacket.getData());
                    message.process(TreeNode.this);
                } catch (IOException | ClassNotFoundException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }
}
