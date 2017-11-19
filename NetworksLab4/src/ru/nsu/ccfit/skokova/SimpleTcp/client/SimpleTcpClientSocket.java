package ru.nsu.ccfit.skokova.SimpleTcp.client;

import org.codehaus.jackson.map.ObjectMapper;
import ru.nsu.ccfit.skokova.SimpleTcp.message.*;
import ru.nsu.ccfit.skokova.SimpleTcp.message.idGenerator.IdGenerator;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

public class SimpleTcpClientSocket {
    private static final int BUF_SIZE = 256;
    private static final int QUEUE_SIZE = 1024;
    private static final int PACK_SIZE = 256;
    private static final long LAST_MSG = -1;
    private static final int SLEEP_TIME = 2000;
    private DatagramSocket datagramSocket;
    private ArrayBlockingQueue<DatagramPacket> inPackets = new ArrayBlockingQueue<>(QUEUE_SIZE);
    private ArrayBlockingQueue<DatagramPacket> outPackets = new ArrayBlockingQueue<>(QUEUE_SIZE);

    private String inetAddress;
    private int port;

    private String myAddress = "127.0.0.1"; //TODO : all info about client server should get from datagram packet
    private int myPort = 3248;

    private byte[] inBuffer = new byte[BUF_SIZE];
    private byte[] outBuffer = new byte[BUF_SIZE];

    private Thread sender;
    private Thread receiver;

    private IdGenerator idGenerator;

    public SimpleTcpClientSocket() {
        try {
            idGenerator = new IdGenerator();
            datagramSocket = new DatagramSocket(new InetSocketAddress(myAddress, myPort));
            sender = new Thread(new Sender());
            receiver = new Thread(new Receiver());
            sender.start();
            receiver.start();
        } catch (SocketException e) {
            System.out.println(e.getMessage());
        }
    }

    public SimpleTcpClientSocket(String inetAddress, int port) {
        this();
        connect(inetAddress, port);
    }

    public void connect(String inetAddress, int port) {
        this.inetAddress = inetAddress;
        this.port = port;
        try {
            Message message = new ConnectMessage(myAddress, myPort);
            message.setId(idGenerator.newId());
            ObjectMapper objectMapper = new ObjectMapper();
            byte[] messageBytes = objectMapper.writeValueAsBytes(message);
            DatagramPacket datagramPacket = new DatagramPacket(messageBytes, messageBytes.length, new InetSocketAddress(inetAddress, port));
            addToSender(datagramPacket);
            System.out.println("Connecting");
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void send(byte[] messageBytes) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            if (messageBytes.length < PACK_SIZE) {
                DataMessage dataMessage = new DataMessage(messageBytes, myAddress, myPort);
                dataMessage.setId(idGenerator.newId());
                byte[] msg = objectMapper.writeValueAsBytes(dataMessage);
                //System.out.println("Sending " + msg.toString());
                DatagramPacket datagramPacket = new DatagramPacket(msg, msg.length, new InetSocketAddress(inetAddress, port));
                addToSender(datagramPacket);
            } else {
                int packetsCount = messageBytes.length / PACK_SIZE;
                int rest = messageBytes.length - PACK_SIZE * packetsCount;
                int i;
                long nextUUID = idGenerator.newId();
                for (i = 0; i < packetsCount * PACK_SIZE; i += PACK_SIZE) {
                    byte[] data = Arrays.copyOfRange(messageBytes, i, i + PACK_SIZE - 1);
                    DataMessage dataMessage = new DataMessage(data, myAddress, myPort);
                    dataMessage.setId(nextUUID);
                    nextUUID = idGenerator.newId();
                    dataMessage.setNextId(nextUUID);
                    byte[] msg = objectMapper.writeValueAsBytes(dataMessage);
                    //System.out.println("Sending " + msg.toString());
                    DatagramPacket datagramPacket = new DatagramPacket(msg, msg.length, new InetSocketAddress(inetAddress, port));
                    addToSender(datagramPacket);
                }
                byte[] data = Arrays.copyOfRange(messageBytes, i, i + rest);
                DataMessage dataMessage = new DataMessage(data, myAddress, myPort);
                dataMessage.setId(nextUUID);
                dataMessage.setNextId(LAST_MSG);
                byte[] msg = objectMapper.writeValueAsBytes(dataMessage);
                //System.out.println("Sending " + msg.toString());
                DatagramPacket datagramPacket = new DatagramPacket(msg, msg.length, new InetSocketAddress(inetAddress, port));
                addToSender(datagramPacket);
            }
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void sendLong(long value) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.putLong(value);
            byte[] bytes = buffer.array();
            DataMessage dataMessage = new DataMessage(bytes, myAddress, myPort);
            dataMessage.setId(idGenerator.newId());
            ObjectMapper objectMapper = new ObjectMapper();
            byte[] msg = objectMapper.writeValueAsBytes(dataMessage);
            DatagramPacket datagramPacket = new DatagramPacket(msg, msg.length, new InetSocketAddress(inetAddress, port));
            addToSender(datagramPacket);
        } catch (InterruptedException e) {
            System.err.println("Interrupted");
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public void sendInt(int value) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
            buffer.putInt(value);
            byte[] bytes = buffer.array();
            DataMessage dataMessage = new DataMessage(bytes, myAddress, myPort);
            dataMessage.setId(idGenerator.newId());
            ObjectMapper objectMapper = new ObjectMapper();
            byte[] msg = objectMapper.writeValueAsBytes(dataMessage);
            DatagramPacket datagramPacket = new DatagramPacket(msg, msg.length, new InetSocketAddress(inetAddress, port));
            addToSender(datagramPacket);
        } catch (InterruptedException e) {
            System.err.println("Interrupted");
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public long receive(byte[] result, int startOffset, int length) {
        byte[] messageBytes;
        DataMessage dataMessage;
        long count = 0;
        try {
            DatagramPacket datagramPacket = inPackets.take();
            messageBytes = datagramPacket.getData();
            ObjectMapper objectMapper = new ObjectMapper();
            Message message = objectMapper.readValue(messageBytes, Message.class);
            if (message.getClass().getSimpleName().equals("DataMessage")) { //TODO : rewrite
                dataMessage = (DataMessage) message;
                long nextUuid = dataMessage.getNextId();
                for (byte b : dataMessage.getData()) {
                    result[startOffset] = b;
                    startOffset++;
                    count++;
                }
                while (dataMessage.getNextId() != LAST_MSG) {
                    DatagramPacket packet = inPackets.take();
                    messageBytes = packet.getData();
                    dataMessage = (DataMessage) objectMapper.readValue(new String(messageBytes, StandardCharsets.UTF_8), Message.class);
                    if (dataMessage.getId() != nextUuid) {
                        continue;
                    }
                    nextUuid = dataMessage.getNextId();
                    for (byte b : dataMessage.getData()) {
                        result[startOffset] = b;
                        startOffset++;
                        count++;
                    }
                }
            }
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return count;
    }

    public void close() throws IOException {
        try {
            DisconnectMessage disconnectMessage = new DisconnectMessage();
            long disconnectMessageId = idGenerator.newId();
            disconnectMessage.setId(disconnectMessageId);
            ObjectMapper objectMapper = new ObjectMapper();
            byte[] messageBytes = objectMapper.writeValueAsBytes(disconnectMessage);
            DatagramPacket datagramPacket = new DatagramPacket(messageBytes, messageBytes.length, new InetSocketAddress(inetAddress, port));
            addToSender(datagramPacket);
            while (true) {
                DatagramPacket ackPacket = inPackets.peek();
                //DatagramPacket ackPacket = inPackets.take();
                if (ackPacket == null) {
                    continue;
                }
                byte[] ackMessageBytes = ackPacket.getData();
                Message message = objectMapper.readValue(ackMessageBytes, Message.class);
                if (message.getClass().getSimpleName().equals("AckMessage")) {
                    AckMessage ackMessage = (AckMessage) message;
                    if (ackMessage.getAckId() == disconnectMessageId) {
                        break;
                    }
                    //inPackets.put(ackPacket);
                }
                Thread.sleep(SLEEP_TIME);
            }
            sender.interrupt();
            receiver.interrupt();
            datagramSocket.close();
        } catch (InterruptedException e) {
            System.err.println("Interrupted");
        }
    }

    private byte[] wrapBytes(byte[] bytes) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        DataMessage dataMessage = new DataMessage(bytes, myAddress, myPort);
        return objectMapper.writeValueAsBytes(dataMessage);
    }

    private void addToSender(DatagramPacket datagramPacket) throws InterruptedException {
        outPackets.put(datagramPacket);
    }

    class Sender implements Runnable {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    DatagramPacket datagramPacket = outPackets.take();
                    datagramSocket.send(datagramPacket);
                } catch (InterruptedException e) {
                    System.err.println("Interrupted");
                    break;
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                    break;
                }
            }
        }
    }

    class Receiver implements Runnable {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    DatagramPacket datagramPacket = new DatagramPacket(outBuffer, outBuffer.length);
                    datagramSocket.receive(datagramPacket);
                    inPackets.put(datagramPacket);
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                } catch (InterruptedException e) {
                    System.err.println("Interrupted");
                }
            }
        }
    }
}
