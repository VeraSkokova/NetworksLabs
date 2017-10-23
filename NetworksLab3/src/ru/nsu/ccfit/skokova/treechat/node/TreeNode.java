package ru.nsu.ccfit.skokova.treechat.node;

import ru.nsu.ccfit.skokova.treechat.PacketWrapper;
import ru.nsu.ccfit.skokova.treechat.messages.*;
import ru.nsu.ccfit.skokova.treechat.serialization.Serializer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class TreeNode {
    private static final int BUF_SIZE = 2048;
    private static final int QUEUE_SIZE = 100;
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
    private CopyOnWriteArrayList<PacketWrapper> messagesToSend = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<UUID> sentMessages = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<Message> messageHistory = new CopyOnWriteArrayList<>();

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

            addDisconnectionHandler();

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

    private void addDisconnectionHandler() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (parentInetSocketAddress != null) {
                    sendDirectMessage(new UnjoinMessage(), parentInetSocketAddress);
                }
                if (!neighbourAddresses.isEmpty()) {
                    takeCareOfChildren();
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            } catch (InterruptedException e) {
                System.out.println("Interrupted");
            }
        }));
    }

    private void takeCareOfChildren() throws IOException, InterruptedException {
        if (parentInetSocketAddress != null) {
            sendMessage(new NewParentMessage(myInetSocketAddress, parentInetSocketAddress), parentInetSocketAddress);
        } else {
            sendMessage(new NewParentMessage(myInetSocketAddress, neighbourAddresses.get(0)));
        }
    }

    public CopyOnWriteArrayList<InetSocketAddress> getNeighbourAddresses() {
        return neighbourAddresses;
    }

    public CopyOnWriteArrayList<PacketWrapper> getMessagesToSend() {
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
                addToSender(new PacketWrapper(message.getUUID(), new DatagramPacket(messageBytes, messageBytes.length, inetSocketAddress)));
            }
        }
        sentMessages.add(message.getUUID());
        addToHistory(message);
    }

    public void sendMessage(Message message, InetSocketAddress previousAuthor) throws IOException, InterruptedException {
        byte[] messageBytes = Serializer.serialize(message);
        for (InetSocketAddress inetSocketAddress : getNeighbourAddresses()) {
            if ((!isAuthor(inetSocketAddress, message)) && (!inetSocketAddress.equals(previousAuthor))) {
                addToSender(new PacketWrapper(message.getUUID(), new DatagramPacket(messageBytes, messageBytes.length, inetSocketAddress)));
            }
        }
        sentMessages.add(message.getUUID());
        addToHistory(message);

    }

    public void sendDirectMessage(Message message, InetSocketAddress receiver) throws IOException, InterruptedException {
        byte[] messageBytes = Serializer.serialize(message);
        PacketWrapper packetWrapper = new PacketWrapper(message.getUUID(), new DatagramPacket(messageBytes, messageBytes.length, receiver));
        if (message.getClass().getSimpleName().equals("AckMessage")) {
            packetWrapper.setAck(true);
        }
        addToSender(packetWrapper);
        sentMessages.add(message.getUUID());
        addToHistory(message);

    }

    private boolean isAuthor(InetSocketAddress inetSocketAddress, Message message) {
        return (message.getSenderInetSocketAddress().equals(inetSocketAddress));
    }

    private void addToHistory(Message message) {
        if (!messageHistory.contains(message)) {
            messageHistory.add(message);
        }
    }

    private void addToSender(PacketWrapper packetWrapper) {
        if (!messagesToSend.contains(packetWrapper)) {
            messagesToSend.add(packetWrapper);
        }
    }

    class Sender implements Runnable {

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    if (!messagesToSend.isEmpty()) {
                        PacketWrapper packetWrapper = messagesToSend.get(0);
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - packetWrapper.getLastAttempt() > PacketWrapper.DIFF) {
                            DatagramPacket message = packetWrapper.getDatagramPacket();
                            socket.send(message);
                            messagesToSend.get(0).setLastAttempt(currentTime);
                            if (packetWrapper.isAck()) {
                                messagesToSend.remove(packetWrapper);
                            }
                        } else {
                            Thread.sleep(currentTime - packetWrapper.getLastAttempt());
                        }
                    }
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                } catch (InterruptedException e) {
                    System.out.println("Interrupted");
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
                        if (!messageHistory.contains(message)) {
                            //System.out.println("Received " + message);
                            message.process(TreeNode.this);
                        }
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
