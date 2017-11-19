package ru.nsu.ccfit.skokova.SimpleTcp.server;

import org.codehaus.jackson.map.ObjectMapper;
import ru.nsu.ccfit.skokova.SimpleTcp.message.AckMessage;
import ru.nsu.ccfit.skokova.SimpleTcp.message.ConnectMessage;
import ru.nsu.ccfit.skokova.SimpleTcp.message.DataMessage;
import ru.nsu.ccfit.skokova.SimpleTcp.message.Message;
import ru.nsu.ccfit.skokova.SimpleTcp.message.utils.MessageUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class SimpleTcpServerSocket {
    private static final int QUEUE_SIZE = 256;
    private static final int PACK_SIZE = 1024;

    private int port;
    private DatagramSocket datagramServerSocket;

    private BlockingQueue<InetSocketAddress> incomingRequests = new ArrayBlockingQueue<>(QUEUE_SIZE);
    private BlockingQueue<DatagramPacket> outPackets = new ArrayBlockingQueue<>(QUEUE_SIZE);
    private BlockingQueue<DatagramPacket> inPackets = new ArrayBlockingQueue<>(QUEUE_SIZE);
    private List<SocketSimulator> socketSimulators = new CopyOnWriteArrayList<>();

    public SimpleTcpServerSocket(int port) {
        try {
            this.port = port;
            datagramServerSocket = new DatagramSocket(port);
            Thread receiver = new Thread(new Receiver());
            Thread sender = new Thread(new Sender());
            receiver.start();
            sender.start();
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
            socketSimulator = new SocketSimulator(this, inetSocketAddress);
            socketSimulators.add(socketSimulator);
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

    private void sendAck(long ackId, String hostName, int port, ObjectMapper objectMapper) throws IOException, InterruptedException {
        AckMessage ackMessage = new AckMessage(ackId);
        byte[] messageBytes = objectMapper.writeValueAsBytes(ackMessage);
        DatagramPacket datagramPacket = new DatagramPacket(messageBytes, messageBytes.length, new InetSocketAddress(hostName, port));
        outPackets.put(datagramPacket);
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
            while (!Thread.interrupted()) {
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
    }

    class Receiver implements Runnable {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    DatagramPacket datagramPacket = new DatagramPacket(new byte[PACK_SIZE], PACK_SIZE);
                    datagramServerSocket.receive(datagramPacket);
                    process(datagramPacket);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }

        private void process(byte[] bytes, int port, String hostName) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                //Message message = objectMapper.readValue(bytes, Message.class);
                Message message = MessageUtils.getHeader(bytes);
                if (message.getClass().getSimpleName().equals("ConnectMessage")) { //TODO : rewrite
                    ConnectMessage connectMessage = (ConnectMessage) message;
                    InetSocketAddress inetSocketAddress = new InetSocketAddress(hostName, port);
                    incomingRequests.put(inetSocketAddress);
                } else if (message.getClass().getSimpleName().equals("DataMessage")) {
                    DataMessage dataMessage = (DataMessage) message;
                    /*String dataMessageString = objectMapper.writeValueAsString(dataMessage);
                    System.out.println(dataMessageString);*/
                    SocketSimulator tempSocketSimulator = new SocketSimulator(SimpleTcpServerSocket.this, new InetSocketAddress(hostName, port));
                    int receiverIndex = socketSimulators.indexOf(tempSocketSimulator);
                    DataWrapper dataWrapper = new DataWrapper(dataMessage, MessageUtils.getData(bytes, bytes[0], dataMessage.getDataLength()));
                    socketSimulators.get(receiverIndex).addMessage(dataWrapper);
                } else if (message.getClass().getSimpleName().equals("DisconnectMessage")) {
                    SocketSimulator tempSocketSimulator = new SocketSimulator(SimpleTcpServerSocket.this, new InetSocketAddress(hostName, port));
                    int receiverIndex = socketSimulators.indexOf(tempSocketSimulator);
                    sendAck(message.getId(), hostName, port, objectMapper);
                    socketSimulators.remove(receiverIndex);
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            } catch (InterruptedException e) {
                System.err.println("Interrupted");
            }
        }

        private void process(DatagramPacket datagramPacket) {
            byte[] messageBytes = datagramPacket.getData();
            int port = datagramPacket.getPort();
            String hostName = datagramPacket.getAddress().getHostName();
            process(messageBytes, port, hostName);
        }
    }
}
