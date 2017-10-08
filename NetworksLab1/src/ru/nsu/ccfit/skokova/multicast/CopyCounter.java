package ru.nsu.ccfit.skokova.multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.HashMap;

public class CopyCounter {
    private static final long TIME = 2000;
    private static final String HELLO = "Hello";
    private static final int SIZE = 16;
    private static final int PORT = 4600;
    private static final int ATTEMPTS = 3;
    private HashMap<String, Integer> ipAddresses = new HashMap<>();
    private MulticastSocket multicastSocket;
    private InetAddress groupAddress;
    private String address;

    public CopyCounter(String address) {
        this.address = address;
    }

    public void runProgram() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopProgram));
        try {
            multicastSocket = new MulticastSocket(PORT);
            groupAddress = InetAddress.getByName(address);
            multicastSocket.joinGroup(groupAddress);

            while (true) {
                sendHelloMessage();
                System.out.println("Sending hello");
                long sendTime = System.currentTimeMillis();
                for (String key : ipAddresses.keySet()) {
                    ipAddresses.replace(key, ipAddresses.get(key), ipAddresses.get(key) - 1);
                }
                while (timeIsOver(sendTime)) {
                    try {
                        multicastSocket.setSoTimeout((int) TIME);
                        DatagramPacket packet = new DatagramPacket(new byte[SIZE], SIZE);
                        multicastSocket.receive(packet);
                        System.out.println("Received " + packet.toString());
                        processPacket(packet);
                    } catch (SocketTimeoutException e) {
                        break;
                    }
                }
                //multicastSocket.setSoTimeout(0);
                for (String key : ipAddresses.keySet()) {
                    if (ipAddresses.get(key) == 0) {
                        ipAddresses.remove(key);
                        System.out.println(ipAddresses.keySet().toString());
                    }
                }
                Thread.sleep(TIME);
                System.out.println("Sleep");
            }
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
        }

    }

    private void stopProgram() {
        try {
            multicastSocket.leaveGroup(groupAddress);
            multicastSocket.close();
            System.out.println("Left group");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void sendHelloMessage() throws IOException {
        DatagramPacket datagramPacket = new DatagramPacket(HELLO.getBytes(), HELLO.length(), groupAddress, PORT);
        multicastSocket.send(datagramPacket);
    }

    private void processPacket(DatagramPacket datagramPacket) throws IOException {
        if (!ipAddresses.containsKey(datagramPacket.getAddress().getHostAddress())) {
            ipAddresses.put(datagramPacket.getAddress().getHostAddress(), ATTEMPTS);
            System.out.println("Added " + datagramPacket.getAddress().getHostAddress());
            System.out.println(ipAddresses.keySet().toString());
        } else {
            ipAddresses.replace(datagramPacket.getAddress().getHostName(), ATTEMPTS);
            System.out.println("It's not a new client");
        }
    }

    private boolean timeIsOver(long sendTime) {
        return System.currentTimeMillis() - sendTime < TIME;
    }
}
