package ru.nsu.ccfit.skokova.SimpleTcp.client;

import org.apache.commons.lang.ArrayUtils;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class SimpleTcpClientSocket {
    private static final int BUF_SIZE = 256;
    private static final int QUEUE_SIZE = 1024;
    private static final int PACK_SIZE = 64;
    private DatagramSocket datagramSocket;
    private BlockingQueue<DatagramPacket> inPackets = new ArrayBlockingQueue<>(QUEUE_SIZE);
    private BlockingQueue<DatagramPacket> outPackets = new ArrayBlockingQueue<>(QUEUE_SIZE);

    private String inetAddress;
    private int port;

    private String myAddress = "localhost";
    private int myPort = 3248;

    private byte[] inBuffer = new byte[BUF_SIZE];
    private byte[] outBuffer = new byte[BUF_SIZE];

    public SimpleTcpClientSocket() {
        try {
            datagramSocket = new DatagramSocket(new InetSocketAddress(myAddress, myPort));
            Thread sender = new Thread(new Sender());
            Thread receiver = new Thread(new Receiver());
            sender.start();
            receiver.start();
        } catch (SocketException e) {
            System.out.println(e.getMessage());
        }
    }

    public SimpleTcpClientSocket(String inetAddress, int port) {
        connect(inetAddress, port);
    }

    public void connect(String inetAddress, int port) {
        this.inetAddress = inetAddress;
        this.port = port;
        try {
            Message message = new ConnectMessage(myAddress, myPort);
            ObjectMapper objectMapper = new ObjectMapper();
            String messageString = objectMapper.writeValueAsString(message);
            DatagramPacket datagramPacket = new DatagramPacket(messageString.getBytes(), messageString.getBytes().length, new InetSocketAddress(inetAddress, port));
            outPackets.put(datagramPacket);
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
                DataMessage dataMessage = new DataMessage(messageBytes);
                byte[] msg = objectMapper.writeValueAsBytes(dataMessage);
                DatagramPacket datagramPacket = new DatagramPacket(msg, msg.length);
                outPackets.put(datagramPacket);
            } else {
                int packetsCount = messageBytes.length / PACK_SIZE;
                int rest = messageBytes.length - PACK_SIZE * packetsCount;
                int i;
                UUID nextUUID = UUID.randomUUID();
                for (i = 0; i < packetsCount * PACK_SIZE; i += PACK_SIZE) {
                    byte[] data = Arrays.copyOfRange(messageBytes, i, i + PACK_SIZE - 1);
                    DataMessage dataMessage = new DataMessage(data);
                    dataMessage.setId(nextUUID);
                    nextUUID = UUID.randomUUID();
                    dataMessage.setNextId(nextUUID);
                    byte[] msg = objectMapper.writeValueAsBytes(dataMessage);
                    DatagramPacket datagramPacket = new DatagramPacket(msg, msg.length);
                    outPackets.put(datagramPacket);
                }
                byte[] data = Arrays.copyOfRange(messageBytes, i, i + rest);
                DataMessage dataMessage = new DataMessage(data);
                dataMessage.setId(nextUUID);
                dataMessage.setNextId(null);
                byte[] msg = objectMapper.writeValueAsBytes(dataMessage);
                DatagramPacket datagramPacket = new DatagramPacket(msg, msg.length);
                outPackets.put(datagramPacket);
            }
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        } catch (IOException e) {
            System.out.println(e.getMessage());
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
            Message message = objectMapper.readValue(new String(messageBytes, StandardCharsets.UTF_8), Message.class);
            if (message.getClass().getSimpleName().equals("DataMessage")) { //TODO : rewrite
                dataMessage = (DataMessage)message;
                UUID nextUuid = dataMessage.getNextId();
                for (byte b : dataMessage.getData()) {
                    result[startOffset] = b;
                    startOffset++;
                    count++;
                }
                while (dataMessage.getNextId() != null) {
                    DatagramPacket packet = inPackets.take();
                    messageBytes = packet.getData();
                    dataMessage = (DataMessage) objectMapper.readValue(new String(messageBytes, StandardCharsets.UTF_8), Message.class);
                    if (!dataMessage.getId().equals(nextUuid)) {
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

    public void close() {

    }

    private byte[] wrapBytes(byte[] bytes) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        DataMessage dataMessage = new DataMessage(bytes);
        return objectMapper.writeValueAsBytes(dataMessage);
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
}
