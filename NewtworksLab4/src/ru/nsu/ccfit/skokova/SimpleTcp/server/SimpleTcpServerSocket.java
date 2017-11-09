package ru.nsu.ccfit.skokova.SimpleTcp.server;

import org.codehaus.jackson.map.ObjectMapper;
import ru.nsu.ccfit.skokova.SimpleTcp.message.ConnectMessage;
import ru.nsu.ccfit.skokova.SimpleTcp.message.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class SimpleTcpServerSocket {
    private static final int QUEUE_SIZE = 256;
    private static final int PACK_SIZE = 128;

    private int port;
    private DatagramSocket datagramServerSocket;

    private BlockingQueue<InetSocketAddress> incomingRequests = new ArrayBlockingQueue<>(QUEUE_SIZE);

    public SimpleTcpServerSocket(int port) {
        try {
            this.port = port;
            datagramServerSocket = new DatagramSocket(port);
            Thread receiver = new Thread(new Receiver());
            receiver.start();
        } catch (SocketException e) {
            System.out.println(e.getMessage());
        }
    }

    public void accept() {
        try {
            InetSocketAddress inetSocketAddress = incomingRequests.take();
            //clientSocket = new SimpleTcpClientSocket(inetSocketAddress.getAddress().getHostAddress(), inetSocketAddress.getPort());
            System.out.println("Accepted: " + inetSocketAddress.getAddress() + ":" + inetSocketAddress.getPort());
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        }
    }

    public void sendTo(InetSocketAddress inetSocketAddress, byte[] data) {
        try {
            DatagramPacket datagramPacket = new DatagramPacket(data, data.length, inetSocketAddress);
            datagramServerSocket.send(datagramPacket);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    class Receiver implements Runnable {
        @Override
        public void run() {
            try {
                DatagramPacket datagramPacket = new DatagramPacket(new byte[PACK_SIZE], PACK_SIZE);
                datagramServerSocket.receive(datagramPacket);
                byte[] messageBytes = datagramPacket.getData();
                process(messageBytes);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }

        private void process(byte[] bytes) {
            try {
                String messageString = new String(bytes, StandardCharsets.UTF_8);
                ObjectMapper objectMapper = new ObjectMapper();
                Message message = objectMapper.readValue(messageString, Message.class);
                if (message.getClass().getSimpleName().equals("ConnectMessage")) { //TODO : rewrite
                    ConnectMessage connectMessage = (ConnectMessage) message;
                    InetSocketAddress inetSocketAddress = new InetSocketAddress(connectMessage.getHostName(), connectMessage.getPort());
                    incomingRequests.put(inetSocketAddress);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                System.out.println("Interrupted");
            }
        }
    }
}
