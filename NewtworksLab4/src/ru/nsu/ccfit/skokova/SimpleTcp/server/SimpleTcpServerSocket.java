package ru.nsu.ccfit.skokova.SimpleTcp.server;

import ru.nsu.ccfit.skokova.SimpleTcp.client.SimpleTcpClientSocket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class SimpleTcpServerSocket {
    private static final int QUEUE_SIZE = 256;

    private int port;
    private DatagramSocket datagramServerSocket;

    private BlockingQueue<InetSocketAddress> incomingRequests = new ArrayBlockingQueue<>(QUEUE_SIZE);

    public SimpleTcpServerSocket(int port) {
        this.port = port;
    }

    public SimpleTcpClientSocket accept() {
        SimpleTcpClientSocket clientSocket = null;
        try {
            InetSocketAddress inetSocketAddress = incomingRequests.take();
            clientSocket = new SimpleTcpClientSocket(inetSocketAddress.getAddress(), inetSocketAddress.getPort());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return clientSocket;
    }

    class Listener implements Runnable {
        @Override
        public void run() {
            try {
                DatagramPacket datagramPacket = new DatagramPacket(new byte[16], 16);
                datagramServerSocket.receive(datagramPacket);
                byte[] messageBytes = datagramPacket.getData();
                process(messageBytes);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }

        private void process(byte[] bytes) {

        }
    }
}
