package ru.nsu.ccfit.skokova.SimpleTcp.server;

import org.codehaus.jackson.map.ObjectMapper;
import ru.nsu.ccfit.skokova.SimpleTcp.message.ConnectMessage;
import ru.nsu.ccfit.skokova.SimpleTcp.message.DataMessage;
import ru.nsu.ccfit.skokova.SimpleTcp.message.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class SimpleTcpServerSocket {
    private static final int QUEUE_SIZE = 256;
    private static final int PACK_SIZE = 128;

    private int port;
    private DatagramSocket datagramServerSocket;

    private BlockingQueue<InetSocketAddress> incomingRequests = new ArrayBlockingQueue<>(QUEUE_SIZE);
    private BlockingQueue<DatagramPacket> outPackets = new ArrayBlockingQueue<>(QUEUE_SIZE);
    private BlockingQueue<DatagramPacket> inPackets = new ArrayBlockingQueue<>(QUEUE_SIZE);
    private List<SocketSimulator> socketSimulators = new ArrayList<>();

    private Map<SocketSimulator, SortedSet<DataMessage>> bytesForSockets = new HashMap<>(); //other type of collection?

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

    public SocketSimulator accept() {
        SocketSimulator socketSimulator = null;
        try {
            InetSocketAddress inetSocketAddress = incomingRequests.take();
            //clientSocket = new SimpleTcpClientSocket(inetSocketAddress.getAddress().getHostAddress(), inetSocketAddress.getPort());
            System.out.println("Accepted: " + inetSocketAddress.getAddress() + ":" + inetSocketAddress.getPort());
            socketSimulator = new SocketSimulator(this, inetSocketAddress.getAddress().getHostAddress(), inetSocketAddress.getPort());
            socketSimulators.add(socketSimulator);
            SortedSet<DataMessage> sortedMessages = Collections.synchronizedSortedSet(new TreeSet<DataMessage>());
            bytesForSockets.put(socketSimulator, sortedMessages);
        } catch (InterruptedException e) {
            System.out.println("Interrupted");

        }
        return socketSimulator;
    }

    public void send(byte[] data) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Message message = objectMapper.readValue(data, Message.class);
            DatagramPacket datagramPacket = new DatagramPacket(data, data.length, new InetSocketAddress(message.getHostName(), message.getPort()));
            outPackets.put(datagramPacket);
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public TreeSet<DataMessage> receive(SocketSimulator socketSimulator) {
        TreeSet<DataMessage> result = new TreeSet<>();
        result.addAll(bytesForSockets.get(socketSimulator));
        bytesForSockets.get(socketSimulator).clear();
        return result;
        /*ArrayList<Byte> messageBytes = new ArrayList<>();
        for (DataMessage message : bytesForSockets.get(socketSimulator)) {
            for (byte b : message.getData()) {
                messageBytes.add(b);
            }
        }
        Byte[] bytes = messageBytes.toArray(new Byte[bytesForSockets.get(socketSimulator).size()]);
        return ArrayUtils.toPrimitive(bytes);*/
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleTcpServerSocket that = (SimpleTcpServerSocket) o;

        return port == that.port;
    }

    @Override
    public int hashCode() {
        return port;
    }

    class Sender implements Runnable {
        @Override
        public void run() {
            try {
                DatagramPacket datagramPacket = outPackets.take();
                datagramServerSocket.send(datagramPacket);
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
                } else if (message.getClass().getSimpleName().equals("DataMessage")) {
                    DataMessage dataMessage = (DataMessage) message;
                    bytesForSockets.get(new SocketSimulator(SimpleTcpServerSocket.this, message.getHostName(), message.getPort())).add(dataMessage);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                System.out.println("Interrupted");
            }
        }
    }
}
