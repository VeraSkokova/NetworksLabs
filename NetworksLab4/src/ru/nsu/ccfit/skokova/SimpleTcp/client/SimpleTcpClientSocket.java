package ru.nsu.ccfit.skokova.SimpleTcp.client;

import org.codehaus.jackson.map.ObjectMapper;
import ru.nsu.ccfit.skokova.SimpleTcp.message.*;
import ru.nsu.ccfit.skokova.SimpleTcp.message.idGenerator.IdGenerator;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

public class SimpleTcpClientSocket {
    private static final int BUF_SIZE = 256;
    private static final int QUEUE_SIZE = 1024;
    private static final int PACK_SIZE = 64;
    private static final long LAST_MSG = -1;
    private static final int SLEEP_TIME = 2000;
    private static final int SEND_TIME_DIFF = 3000;
    private DatagramSocket datagramSocket;
    private ArrayBlockingQueue<Message> inMessages = new ArrayBlockingQueue<>(QUEUE_SIZE);
    private ArrayBlockingQueue<Message> outMessages = new ArrayBlockingQueue<>(QUEUE_SIZE);

    private String inetAddress;
    private int port;

    private byte[] inBuffer = new byte[BUF_SIZE];
    private byte[] outBuffer = new byte[BUF_SIZE];

    private Thread sender;
    private Thread receiver;

    private boolean requestedConnect;
    private boolean requestedDisconnect;

    private IdGenerator idGenerator;

    public SimpleTcpClientSocket() {
        try {
            idGenerator = new IdGenerator();
            datagramSocket = new DatagramSocket();
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
        this.requestedConnect = true;
        try {
            Message message = new ConnectMessage();
            long connectMessageId = idGenerator.newId();
            message.setId(connectMessageId);
            message.setTime(System.currentTimeMillis());
            ObjectMapper objectMapper = new ObjectMapper();
            /*byte[] messageBytes = objectMapper.writeValueAsBytes(message);
            DatagramPacket datagramPacket = new DatagramPacket(messageBytes, messageBytes.length, new InetSocketAddress(inetAddress, port));*/
            addToSender(message);
            System.out.println("Connecting");
            while (true) {
                if (!requestedConnect) {
                    break;
                }
                Thread.sleep(SLEEP_TIME);
            }
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        }
    }

    public void send(byte[] messageBytes) {
        ObjectMapper objectMapper = new ObjectMapper();
        long time = System.currentTimeMillis();
        try {
            if (messageBytes.length < PACK_SIZE) {
                DataMessage dataMessage = new DataMessage(messageBytes);
                dataMessage.setId(idGenerator.newId());
                dataMessage.setTime(time);
                /*byte[] msg = objectMapper.writeValueAsBytes(dataMessage);
                //System.out.println("Sending " + msg.toString());
                DatagramPacket datagramPacket = new DatagramPacket(msg, msg.length, new InetSocketAddress(inetAddress, port));*/
                addToSender(dataMessage);
            } else {
                int packetsCount = messageBytes.length / PACK_SIZE;
                int rest = messageBytes.length - PACK_SIZE * packetsCount;
                int i;
                long nextUUID = idGenerator.newId();
                for (i = 0; i < packetsCount * PACK_SIZE; i += PACK_SIZE) {
                    byte[] data = Arrays.copyOfRange(messageBytes, i, i + PACK_SIZE);
                    DataMessage dataMessage = new DataMessage(data);
                    dataMessage.setId(nextUUID);
                    nextUUID = idGenerator.newId();
                    dataMessage.setNextId(nextUUID);
                    dataMessage.setTime(time);
                    /*byte[] msg = objectMapper.writeValueAsBytes(dataMessage);
                    //System.out.println("Sending " + msg.toString());
                    DatagramPacket datagramPacket = new DatagramPacket(msg, msg.length, new InetSocketAddress(inetAddress, port));*/
                    addToSender(dataMessage);
                }
                if (rest != 0) {
                    byte[] data = Arrays.copyOfRange(messageBytes, i, i + rest);
                    DataMessage dataMessage = new DataMessage(data);
                    dataMessage.setId(nextUUID);
                    dataMessage.setNextId(LAST_MSG);
                    dataMessage.setTime(time);
                    /*byte[] msg = objectMapper.writeValueAsBytes(dataMessage);
                    //System.out.println("Sending " + msg.toString());
                    DatagramPacket datagramPacket = new DatagramPacket(msg, msg.length, new InetSocketAddress(inetAddress, port));*/
                    addToSender(dataMessage);
                }
            }
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        }
    }

    public void sendLong(long value) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.putLong(value);
            byte[] bytes = buffer.array();
            DataMessage dataMessage = new DataMessage(bytes);
            dataMessage.setId(idGenerator.newId());
            dataMessage.setTime(System.currentTimeMillis());
            /*ObjectMapper objectMapper = new ObjectMapper();
            byte[] msg = objectMapper.writeValueAsBytes(dataMessage);
            DatagramPacket datagramPacket = new DatagramPacket(msg, msg.length, new InetSocketAddress(inetAddress, port));*/
            addToSender(dataMessage);
        } catch (InterruptedException e) {
            System.err.println("Interrupted");
        }
    }

    public void sendInt(int value) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
            buffer.putInt(value);
            byte[] bytes = buffer.array();
            DataMessage dataMessage = new DataMessage(bytes);
            dataMessage.setId(idGenerator.newId());
            dataMessage.setTime(System.currentTimeMillis());
            /*ObjectMapper objectMapper = new ObjectMapper();
            byte[] msg = objectMapper.writeValueAsBytes(dataMessage);
            DatagramPacket datagramPacket = new DatagramPacket(msg, msg.length, new InetSocketAddress(inetAddress, port));*/
            addToSender(dataMessage);
        } catch (InterruptedException e) {
            System.err.println("Interrupted");
        }
    }

    public long receive(byte[] result, int startOffset, int length) {
        byte[] messageBytes;
        DataMessage dataMessage;
        long count = 0;
        try {
            Message message = inMessages.take();
            sendAck(message);
            if (message.getClass().getSimpleName().equals("DataMessage")) {
                dataMessage = (DataMessage) message;
                long nextUuid = dataMessage.getNextId();
                for (byte b : dataMessage.getData()) {
                    result[startOffset] = b;
                    startOffset++;
                    count++;
                }
                while (dataMessage.getNextId() != LAST_MSG) {
                    Message nextMessage = inMessages.take();
                    dataMessage = (DataMessage) nextMessage;
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
            } else {
                inMessages.put(message);
            }
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        }
        return count;
    }

    public void close() throws IOException {
        try {
            DisconnectMessage disconnectMessage = new DisconnectMessage();
            long disconnectMessageId = idGenerator.newId();
            disconnectMessage.setId(disconnectMessageId);
            ObjectMapper objectMapper = new ObjectMapper();
            /*byte[] messageBytes = objectMapper.writeValueAsBytes(disconnectMessage);
            DatagramPacket datagramPacket = new DatagramPacket(messageBytes, messageBytes.length, new InetSocketAddress(inetAddress, port));*/
            addToSender(disconnectMessage);
            requestedDisconnect = true;
            while (true) {
                if (!requestedDisconnect) {
                    break;
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

    public void setTimeout(int timeout) throws SocketException {
        datagramSocket.setSoTimeout(timeout);
    }

    private void addToSender(Message message) throws InterruptedException {
        outMessages.put(message);
    }

    private void sendAck(Message message) throws InterruptedException {
        AckMessage ackMessage = new AckMessage(message.getId());
        ackMessage.setTime(message.getTime());
        addToSender(message);
    }

    private DatagramPacket wrapMessage(Message message) throws IOException {
        byte[] messageBytes = new ObjectMapper().writeValueAsBytes(message);
        return new DatagramPacket(messageBytes, messageBytes.length, new InetSocketAddress(inetAddress, port));
    }

    class Sender implements Runnable {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    Message message = outMessages.take();
                    long now = System.currentTimeMillis();
                    if (now - message.getLastAttempt() > SEND_TIME_DIFF) {
                        DatagramPacket datagramPacket = wrapMessage(message);
                        datagramSocket.send(datagramPacket);
                        message.setLastAttempt(now);
                    }
                    outMessages.put(message);
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
                    process(datagramPacket);
                } catch (SocketTimeoutException e) {
                    System.err.println("Timeout exceeded");
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                } catch (InterruptedException e) {
                    System.err.println("Interrupted");
                }
            }
        }

        private void process(DatagramPacket datagramPacket) throws IOException, InterruptedException {
            byte[] packetData = datagramPacket.getData();
            Message message = new ObjectMapper().readValue(packetData, Message.class);
            if (message.getClass().getSimpleName().equals("AckMessage")) {
                AckMessage ackMessage = (AckMessage) message;
                Message msg = new ConnectMessage();
                msg.setId(ackMessage.getAckId());
                msg.setTime(msg.getTime());
                outMessages.remove(msg);
                if (requestedConnect) {
                    requestedConnect = false;
                }
                if (requestedDisconnect) {
                    requestedDisconnect = false;
                }
            } else {
                inMessages.put(message);
            }
        }
    }
}
