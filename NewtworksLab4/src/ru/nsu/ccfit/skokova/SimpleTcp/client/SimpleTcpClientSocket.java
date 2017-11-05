package ru.nsu.ccfit.skokova.SimpleTcp.client;

import ru.nsu.ccfit.skokova.SimpleTcp.message.MessageType;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class SimpleTcpClientSocket {
    private static final int BUF_SIZE = 256;
    private static final int QUEUE_SIZE = 1024;
    private static final int TYPE_SIZE = 10;
    private DatagramSocket datagramSocket;
    private BlockingQueue<DatagramPacket> inPackets = new ArrayBlockingQueue<>(QUEUE_SIZE);
    private BlockingQueue<DatagramPacket> outPackets = new ArrayBlockingQueue<>(QUEUE_SIZE);

    private InetAddress inetAddress;
    private int port;

    private byte[] inBuffer;
    private byte[] outBuffer;

    public SimpleTcpClientSocket() {
    }

    public SimpleTcpClientSocket(InetAddress inetAddress, int port) {
        connect(inetAddress, port);
    }

    public void connect(InetAddress inetAddress, int port) {
        this.inetAddress = inetAddress;
        this.port = port;
        try {
            MessageType messageType = MessageType.CONNECT;
            byte[] messageBytes = messageType.name().getBytes();
            datagramSocket = new DatagramSocket(port, inetAddress);
            DatagramPacket datagramPacket = new DatagramPacket(messageBytes, TYPE_SIZE, datagramSocket.getInetAddress(), datagramSocket.getPort());
            inPackets.put(datagramPacket);
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        } catch (SocketException e) {
            System.out.println(e.getMessage());
        }

    }

    public void send(byte[] messageBytes) {
        try {
            DatagramPacket datagramPacket = new DatagramPacket(messageBytes, messageBytes.length);
            outPackets.put(datagramPacket);
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        }
    }

    public byte[] receive() {
        byte[] messageBytes = null;
        try {
            DatagramPacket datagramPacket = inPackets.take();
            messageBytes = datagramPacket.getData();
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        }
        return messageBytes;
    }

    public void close() {

    }

    class Sender implements Runnable {
        @Override
        public void run() {
            try {
                DatagramPacket datagramPacket = outPackets.take();
                datagramSocket.send(datagramPacket);
            } catch (InterruptedException e) {
                System.out.println("Interrupted");
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    class Receiver implements Runnable {
        @Override
        public void run() {
            try {
                DatagramPacket datagramPacket = new DatagramPacket(outBuffer, outBuffer.length);
                datagramSocket.receive(datagramPacket);
                inPackets.put(datagramPacket);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            } catch (InterruptedException e) {
                System.out.println("Interrupted");
            }
        }
    }

    /*class InputStreamScanner implements Runnable {
        @Override
        public void run() {
            try {
                int read = inputStream.read(inBuffer, 0, BUF_SIZE);
                DatagramPacket datagramPacket = new DatagramPacket(inBuffer, inBuffer.length, inetAddress, port);
                inPackets.put(datagramPacket);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            } catch (InterruptedException e) {
                System.out.println("Interrupted");
            }
        }
    }

    class OutputStreamScanner implements Runnable {
        @Override
        public void run() {
            try {
                DatagramPacket datagramPacket = outPackets.take();
                byte[] messageBytes = datagramPacket.getData();
                outputStream.write(messageBytes);
            } catch (InterruptedException e) {
                System.out.println("Interrupted");
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }*/
}
