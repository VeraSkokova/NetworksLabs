package ru.nsu.ccfit.skokova.treechat.node;

import ru.nsu.ccfit.skokova.treechat.messages.JoinMessage;
import ru.nsu.ccfit.skokova.treechat.messages.Message;
import ru.nsu.ccfit.skokova.treechat.messages.TextMessage;
import ru.nsu.ccfit.skokova.treechat.serialization.Serializer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class TreeNode {
    private static final int BUF_SIZE = 2048;
    private static final int QUEUE_SIZE = 100;
    private static final Object lock = new Object();
    private DatagramSocket socket;
    private String name;
    private int percentageLoss;
    private CopyOnWriteArrayList<InetSocketAddress> neighbourAddresses = new CopyOnWriteArrayList<>();
    private InetSocketAddress myInetSocketAddress;
    private boolean isRoot;
    private Thread inThread;
    private Thread outThread;
    private InetSocketAddress parentInetSocketAddress;
    private Thread scannerThread;
    private ArrayBlockingQueue<DatagramPacket> messagesToSend = new ArrayBlockingQueue<>(QUEUE_SIZE);
    private CopyOnWriteArrayList<UUID> sentMessages = new CopyOnWriteArrayList<>();

    public TreeNode(String name, int percentageLoss, String address, int port) {
        this.name = name;
        this.percentageLoss = percentageLoss;
        this.myInetSocketAddress = new InetSocketAddress(address, port);
        this.isRoot = true;
    }

    public TreeNode(String name, int percentageLoss, int port, String address, String parentAddress, int parentPort) {
        this(name, percentageLoss, address, port);
        this.parentInetSocketAddress = new InetSocketAddress(parentAddress, parentPort);
        this.isRoot = false;
    }

    public void start() {
        try {
            System.out.println(name + " started");
            socket = new DatagramSocket(myInetSocketAddress.getPort());
            inThread = new Thread(new Receiver());
            outThread = new Thread(new Sender());
            scannerThread = new Thread(new MessageScanner());

            inThread.start();
            outThread.start();
            scannerThread.start();

            joinChat();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
            Thread.currentThread().interrupt();
        }
    }

    public CopyOnWriteArrayList<InetSocketAddress> getNeighbourAddresses() {
        return neighbourAddresses;
    }

    public ArrayBlockingQueue<DatagramPacket> getMessagesToSend() {
        return messagesToSend;
    }

    public String getName() {
        return name;
    }

    public InetSocketAddress getMyInetSocketAddress() {
        return myInetSocketAddress;
    }

    public void setRoot(boolean root) {
        isRoot = root;
    }

    public InetSocketAddress getParentInetSocketAddress() {
        return parentInetSocketAddress;
    }

    public void setParentInetSocketAddress(InetSocketAddress parentInetSocketAddress) {
        this.parentInetSocketAddress = parentInetSocketAddress;
    }

    public CopyOnWriteArrayList<UUID> getSentMessages() {
        return sentMessages;
    }

    private void joinChat() throws IOException, InterruptedException {
        if (!isRoot) {
            neighbourAddresses.add(parentInetSocketAddress);
            sendMessage(new JoinMessage(myInetSocketAddress));
            System.out.println("Connected to parent");
        }
        System.out.println(name + " joined chat");
    }

    public void sendMessage(Message message) throws IOException, InterruptedException {
        byte[] messageBytes = Serializer.serialize(message);
        for (InetSocketAddress inetSocketAddress : getNeighbourAddresses()) {
            if (!isAuthor(inetSocketAddress, message)) {
                getMessagesToSend().put(new DatagramPacket(messageBytes, messageBytes.length, inetSocketAddress));
            }
        }
        sentMessages.add(message.getUUID());
    }

    public void sendMessage(Message message, InetSocketAddress previousAuthor) throws IOException, InterruptedException {
        byte[] messageBytes = Serializer.serialize(message);
        for (InetSocketAddress inetSocketAddress : getNeighbourAddresses()) {
            if ((!isAuthor(inetSocketAddress, message)) && (!inetSocketAddress.equals(previousAuthor))) {
                getMessagesToSend().put(new DatagramPacket(messageBytes, messageBytes.length, inetSocketAddress));
            }
        }
        sentMessages.add(message.getUUID());
    }

    public void sendDirectMessage(Message message, InetSocketAddress receiver) throws IOException, InterruptedException {
        byte[] messageBytes = Serializer.serialize(message);
        getMessagesToSend().put(new DatagramPacket(messageBytes, messageBytes.length, receiver));
        sentMessages.add(message.getUUID());
    }

    private boolean isAuthor(InetSocketAddress inetSocketAddress, Message message) {
        return (message.getSenderInetSocketAddress().equals(inetSocketAddress));
    }

    class Sender implements Runnable {

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    DatagramPacket message = messagesToSend.take();
                    socket.send(message);
                } catch (InterruptedException | IOException e) {
                    System.out.println(e.getMessage());
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
                    if (!isLost()) {
                        Message message = Serializer.deserialize(datagramPacket.getData());
                        //System.out.println("Received " + message);
                        message.process(TreeNode.this);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    System.out.println(e.getMessage());
                }
            }
        }

        private boolean isLost() {
            int randomInt = ThreadLocalRandom.current().nextInt(0, 100);
            return randomInt < percentageLoss;
        }
    }

    class MessageScanner implements Runnable {

        @Override
        public void run() {
            Scanner scanner = new Scanner(System.in);
            while (!Thread.interrupted()) {
                System.out.print("> ");
                String message = scanner.nextLine();
                try {
                    sendMessage(new TextMessage(UUID.randomUUID(), name, message, myInetSocketAddress));
                } catch (InterruptedException e) {
                    System.out.println("Interrupted");
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }
}
