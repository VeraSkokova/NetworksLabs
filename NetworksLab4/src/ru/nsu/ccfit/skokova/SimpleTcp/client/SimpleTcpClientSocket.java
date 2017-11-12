package ru.nsu.ccfit.skokova.SimpleTcp.client;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import ru.nsu.ccfit.skokova.SimpleTcp.message.ConnectMessage;
import ru.nsu.ccfit.skokova.SimpleTcp.message.DataMessage;
import ru.nsu.ccfit.skokova.SimpleTcp.message.Message;
import ru.nsu.ccfit.skokova.SimpleTcp.message.MessageType;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
                for (i = 0; i < packetsCount * PACK_SIZE; i += PACK_SIZE) {
                    byte[] data = Arrays.copyOfRange(messageBytes, i, i + PACK_SIZE - 1);
                    DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
                    outPackets.put(datagramPacket);
                }
                byte[] data = Arrays.copyOfRange(messageBytes, i, i + rest);
                DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
                outPackets.put(datagramPacket);
            }
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        }  catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public byte[] receive() {
        byte[] messageBytes = null;
        try {
            DatagramPacket datagramPacket = inPackets.take();
            messageBytes = datagramPacket.getData();
            ObjectMapper objectMapper = new ObjectMapper();
            Message message = objectMapper.readValue(new String(messageBytes, StandardCharsets.UTF_8), Message.class);
            if (message.getClass().getSimpleName().equals("DataMessage")) { //TODO : rewrite
                DataMessage dataMessage = (DataMessage)message;
                return dataMessage.getData();
            }
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        } catch (IOException e) {
            System.out.println(e.getMessage());
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

    private byte[] wrapBytes(byte[] bytes) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        DataMessage dataMessage = new DataMessage(bytes);
        return objectMapper.writeValueAsBytes(dataMessage);
    }
}
