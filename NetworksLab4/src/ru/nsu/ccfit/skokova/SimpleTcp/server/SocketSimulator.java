package ru.nsu.ccfit.skokova.SimpleTcp.server;

import org.codehaus.jackson.map.ObjectMapper;
import ru.nsu.ccfit.skokova.SimpleTcp.message.DataMessage;
import ru.nsu.ccfit.skokova.SimpleTcp.message.idGenerator.IdGenerator;
import ru.nsu.ccfit.skokova.SimpleTcp.message.utils.MessageUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class SocketSimulator {
    private static final int PACK_SIZE = 64;
    private static final long LAST_MSG = -1;
    private final IdGenerator idGenerator;
    private SimpleTcpServerSocket serverSocket;
    private String inetAddress;
    private int port;
    private InetSocketAddress inetSocketAddress;
    private BlockingQueue<DataWrapper> messages = new PriorityBlockingQueue<>();

    public SocketSimulator(SimpleTcpServerSocket serverSocket, String inetAddress, int port) {
        this.serverSocket = serverSocket;
        this.inetAddress = inetAddress;
        this.port = port;
        this.inetSocketAddress = new InetSocketAddress(inetAddress, port);
        this.idGenerator = new IdGenerator();
    }

    public SocketSimulator(SimpleTcpServerSocket serverSocket, InetSocketAddress inetSocketAddress) {
        this.serverSocket = serverSocket;
        this.inetSocketAddress = inetSocketAddress;
        this.port = inetSocketAddress.getPort();
        this.inetAddress = inetSocketAddress.getHostName();
        this.idGenerator = new IdGenerator();
    }

    public void send(byte[] messageBytes) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            if (messageBytes.length < PACK_SIZE) {
                DataMessage dataMessage = new DataMessage(inetAddress, port, messageBytes.length);
                dataMessage.setHostName(inetAddress);
                dataMessage.setPort(port);
                dataMessage.setNextId(LAST_MSG);
                byte[] header = objectMapper.writeValueAsBytes(dataMessage);
                byte[] msgBytes = MessageUtils.createDataMessage(header, messageBytes);
                serverSocket.send(header);
            } else {
                int packetsCount = messageBytes.length / PACK_SIZE;
                int rest = messageBytes.length - PACK_SIZE * packetsCount;
                int i;
                long nextUUID = idGenerator.newId();
                for (i = 0; i < packetsCount * PACK_SIZE; i += PACK_SIZE) {
                    byte[] data = Arrays.copyOfRange(messageBytes, i, i + PACK_SIZE - 1);
                    DataMessage dataMessage = new DataMessage(inetAddress, port, data.length);
                    dataMessage.setHostName(inetAddress);
                    dataMessage.setPort(port);
                    dataMessage.setId(nextUUID);
                    nextUUID = idGenerator.newId();
                    dataMessage.setNextId(nextUUID);
                    byte[] header = objectMapper.writeValueAsBytes(dataMessage);
                    byte[] msgBytes = MessageUtils.createDataMessage(header, data);
                    serverSocket.send(msgBytes);
                }
                byte[] data = Arrays.copyOfRange(messageBytes, i, i + rest);
                DataMessage dataMessage = new DataMessage(inetAddress, port, data.length);
                dataMessage.setHostName(inetAddress);
                dataMessage.setPort(port);
                dataMessage.setId(nextUUID);
                dataMessage.setNextId(LAST_MSG);
                byte[] header = objectMapper.writeValueAsBytes(dataMessage);
                byte[] msgBytes = MessageUtils.createDataMessage(header, data);
                serverSocket.send(msgBytes);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public int receive(byte[] result, long startOffset, long length) {
        int count = 0;
        try {
            do {
                DataWrapper dataWrapper = messages.take();
                byte[] bytes = dataWrapper.getData();
                for (byte b : bytes) {
                    result[(int) startOffset] = b;
                    startOffset++;
                    count++;
                    if (count == length) {
                        break;
                    }
                }
            } while ((messages.peek() != null) && (messages.peek().getDataMessage().getNextId() != LAST_MSG) && (count < length));
        } catch (InterruptedException e) {
            System.err.println("Interrupted");
        }
        return count;
    }

    public long receiveLong() {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        try {
            byte[] bytes = messages.take().getData();
            buffer.put(bytes);
            buffer.flip();//need flip
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return buffer.getLong();
    }

    public int receiveInt() {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        try {
            byte[] bytes = messages.take().getData();
            buffer.put(bytes);
            buffer.flip();
        } catch (InterruptedException e) {
            System.err.println("Interrupted");
        }
        return buffer.getInt();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SocketSimulator that = (SocketSimulator) o;

        if (serverSocket != null ? !serverSocket.equals(that.serverSocket) : that.serverSocket != null) return false;
        return inetAddress != null ? inetAddress.equals(that.inetAddress) : that.inetAddress == null;
    }

    @Override
    public int hashCode() {
        int result = serverSocket != null ? serverSocket.hashCode() : 0;
        result = 31 * result + (inetAddress != null ? inetAddress.hashCode() : 0);
        return result;
    }

    void addMessage(DataWrapper dataWrapper) {
        try {
            if (!messages.contains(dataWrapper)) {
                messages.put(dataWrapper);
            }
        } catch (InterruptedException e) {
            System.err.println("Interrupted");
        }
    }
}
