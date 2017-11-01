package ru.nsu.ccfit.skokova.SimpleTCP.client;

import ru.nsu.ccfit.skokova.SimpleTCP.message.MessageTypes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class SimpleTcpClientSocket {
    private static final int BUF_SIZE = 256;
    private static final int QUEUE_SIZE = 1024;
    private DatagramSocket datagramSocket;
    private BlockingQueue<DatagramPacket> inPackets = new ArrayBlockingQueue<>(QUEUE_SIZE);
    private BlockingQueue<DatagramPacket> outPackets = new ArrayBlockingQueue<>(QUEUE_SIZE);

    private InetAddress inetAddress;
    private int port;

    private InputStream inputStream;
    private OutputStream outputStream;

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
            MessageTypes messageType = MessageTypes.CONNECT;
            byte[] messageBytes = messageType.name().getBytes();
            datagramSocket = new DatagramSocket(port, inetAddress);
            DatagramPacket datagramPacket = new DatagramPacket(messageBytes, messageBytes.length, datagramSocket.getInetAddress(), datagramSocket.getPort());
            inPackets.put(datagramPacket);
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        } catch (SocketException e) {
            System.out.println(e.getMessage());
        }

    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    class Sender implements Runnable {
        @Override
        public void run() {
            try {
                DatagramPacket datagramPacket = inPackets.take();
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
                outPackets.put(datagramPacket);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            } catch (InterruptedException e) {
                System.out.println("Interrupted");
            }
        }
    }

    class InputStreamScanner implements Runnable {
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
    }
}
