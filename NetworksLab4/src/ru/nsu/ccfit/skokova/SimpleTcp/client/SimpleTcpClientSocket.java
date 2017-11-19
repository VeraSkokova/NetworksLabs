package ru.nsu.ccfit.skokova.SimpleTcp.client;

import org.codehaus.jackson.map.ObjectMapper;
import ru.nsu.ccfit.skokova.SimpleTcp.message.*;
import ru.nsu.ccfit.skokova.SimpleTcp.message.idGenerator.IdGenerator;
import ru.nsu.ccfit.skokova.SimpleTcp.message.utils.MessageUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

public class SimpleTcpClientSocket {
    private static final int BUF_SIZE = 256;
    private static final int QUEUE_SIZE = 1024;
    private static final int PACK_SIZE = 64;
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
            byte[] header = objectMapper.writeValueAsBytes(message);
            byte[] msgBytes = MessageUtils.createServiceMessage(header);
            DatagramPacket datagramPacket = new DatagramPacket(msgBytes, msgBytes.length, new InetSocketAddress(inetAddress, port));
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
                DataMessage dataMessage = new DataMessage(myAddress, myPort, messageBytes.length);
                dataMessage.setId(idGenerator.newId());
                byte[] header = objectMapper.writeValueAsBytes(dataMessage);
                byte[] msgBytes = MessageUtils.createDataMessage(header, messageBytes);
                //System.out.println("Sending " + msg.toString());
                DatagramPacket datagramPacket = new DatagramPacket(msgBytes, msgBytes.length, new InetSocketAddress(inetAddress, port));
                addToSender(datagramPacket);
            } else {
                int packetsCount = messageBytes.length / PACK_SIZE;
                int rest = messageBytes.length - PACK_SIZE * packetsCount;
                int i;
                long nextUUID = idGenerator.newId();
                for (i = 0; i < packetsCount * PACK_SIZE; i += PACK_SIZE) {
                    byte[] data = Arrays.copyOfRange(messageBytes, i, i + PACK_SIZE - 1);
                    DataMessage dataMessage = new DataMessage(myAddress, myPort, data.length);
                    dataMessage.setId(nextUUID);
                    nextUUID = idGenerator.newId();
                    dataMessage.setNextId(nextUUID);
                    byte[] header = objectMapper.writeValueAsBytes(dataMessage);
                    byte[] msgBytes = MessageUtils.createDataMessage(header, data);
                    //System.out.println("Sending " + msg.toString());
                    DatagramPacket datagramPacket = new DatagramPacket(msgBytes, msgBytes.length, new InetSocketAddress(inetAddress, port));
                    addToSender(datagramPacket);
                }
                byte[] data = Arrays.copyOfRange(messageBytes, i, i + rest);
                DataMessage dataMessage = new DataMessage(myAddress, myPort, data.length);
                dataMessage.setId(nextUUID);
                dataMessage.setNextId(LAST_MSG);
                byte[] header = objectMapper.writeValueAsBytes(dataMessage);
                //System.out.println("Sending " + msg.toString());
                byte[] msgBytes = new byte[1 + header.length + data.length];
                msgBytes[0] = (byte) header.length;
                MessageUtils.fillMessageBytes(msgBytes, 1, header);
                MessageUtils.fillMessageBytes(msgBytes, 1 + header.length, data);
                DatagramPacket datagramPacket = new DatagramPacket(msgBytes, msgBytes.length, new InetSocketAddress(inetAddress, port));
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
            DataMessage dataMessage = new DataMessage(myAddress, myPort, bytes.length);
            dataMessage.setId(idGenerator.newId());
            ObjectMapper objectMapper = new ObjectMapper();
            byte[] header = objectMapper.writeValueAsBytes(dataMessage);
            byte[] msgBytes = MessageUtils.createDataMessage(header, bytes);
            DatagramPacket datagramPacket = new DatagramPacket(msgBytes, msgBytes.length, new InetSocketAddress(inetAddress, port));
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
            DataMessage dataMessage = new DataMessage(myAddress, myPort, bytes.length);
            dataMessage.setId(idGenerator.newId());
            ObjectMapper objectMapper = new ObjectMapper();
            byte[] header = objectMapper.writeValueAsBytes(dataMessage);
            byte[] msgBytes = MessageUtils.createDataMessage(header, bytes);
            DatagramPacket datagramPacket = new DatagramPacket(msgBytes, msgBytes.length, new InetSocketAddress(inetAddress, port));
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
            int headerLength = messageBytes[0];
            byte[] header = Arrays.copyOfRange(messageBytes, 1, headerLength + 1);
            ObjectMapper objectMapper = new ObjectMapper();
            Message message = MessageUtils.getHeader(header);
            if (message.getClass().getSimpleName().equals("DataMessage")) { //TODO : rewrite
                dataMessage = (DataMessage) message;
                long nextUuid = dataMessage.getNextId();
                byte[] data = Arrays.copyOfRange(messageBytes, 1 + headerLength, messageBytes.length);
                for (byte b : data) {
                    result[startOffset] = b;
                    startOffset++;
                    count++;
                }
                while (dataMessage.getNextId() != LAST_MSG) {
                    DatagramPacket packet = inPackets.take();
                    messageBytes = packet.getData();
                    headerLength = messageBytes[0];
                    header = Arrays.copyOfRange(messageBytes, 1, headerLength + 1);
                    dataMessage = (DataMessage) objectMapper.readValue(header, Message.class);
                    data = Arrays.copyOfRange(messageBytes, 1 + headerLength, messageBytes.length);
                    if (dataMessage.getId() != nextUuid) {
                        continue;
                    }
                    nextUuid = dataMessage.getNextId();
                    for (byte b : data) {
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
            byte[] header = objectMapper.writeValueAsBytes(disconnectMessage);
            byte[] msgBytes = MessageUtils.createServiceMessage(header);
            DatagramPacket datagramPacket = new DatagramPacket(msgBytes, msgBytes.length, new InetSocketAddress(inetAddress, port));
            addToSender(datagramPacket);
            while (true) {
                DatagramPacket ackPacket = inPackets.peek();
                //DatagramPacket ackPacket = inPackets.take();
                if (ackPacket == null) {
                    continue;
                }
                byte[] ackMessageBytes = ackPacket.getData();
                Message message = MessageUtils.getHeader(ackMessageBytes);
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
