package ru.nsu.ccfit.skokova.SimpleTcp.server;

import org.codehaus.jackson.map.ObjectMapper;
import ru.nsu.ccfit.skokova.SimpleTcp.message.DataMessage;

import java.io.IOException;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.UUID;

public class SocketSimulator {
    private static final int PACK_SIZE = 64;
    private SimpleTcpServerSocket serverSocket;
    private String inetAddress;
    private int port;

    private TreeSet<DataMessage> messages = new TreeSet<>();

    public SocketSimulator(SimpleTcpServerSocket serverSocket, String inetAddress, int port) {
        this.serverSocket = serverSocket;
        this.inetAddress = inetAddress;
        this.port = port;
    }

    public void send(byte[] messageBytes) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            if (messageBytes.length < PACK_SIZE) {
                DataMessage dataMessage = new DataMessage(messageBytes);
                dataMessage.setHostName(inetAddress);
                dataMessage.setPort(port);
                dataMessage.setNextId(null);
                byte[] msg = objectMapper.writeValueAsBytes(dataMessage);
                serverSocket.send(msg);
            } else {
                int packetsCount = messageBytes.length / PACK_SIZE;
                int rest = messageBytes.length - PACK_SIZE * packetsCount;
                int i;
                UUID nextUUID = UUID.randomUUID();
                for (i = 0; i < packetsCount * PACK_SIZE; i += PACK_SIZE) {
                    byte[] data = Arrays.copyOfRange(messageBytes, i, i + PACK_SIZE - 1);
                    DataMessage dataMessage = new DataMessage(data);
                    dataMessage.setHostName(inetAddress);
                    dataMessage.setPort(port);
                    dataMessage.setId(nextUUID);
                    nextUUID = UUID.randomUUID();
                    dataMessage.setNextId(nextUUID);
                    byte[] msg = objectMapper.writeValueAsBytes(dataMessage);
                    serverSocket.send(msg);
                }
                byte[] data = Arrays.copyOfRange(messageBytes, i, i + rest);
                DataMessage dataMessage = new DataMessage(data);
                dataMessage.setHostName(inetAddress);
                dataMessage.setPort(port);
                dataMessage.setId(nextUUID);
                dataMessage.setNextId(null);
                byte[] msg = objectMapper.writeValueAsBytes(dataMessage);
                serverSocket.send(msg);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public int receive(byte[] result, int startOffset, int length) {
        int count = 0;
        if (messages.isEmpty()) {
            messages = serverSocket.receive(this);
        }
        do {
            byte[] bytes = messages.first().getData();
            for (byte b : bytes) {
                result[startOffset] = b;
                startOffset++;
                count++;
            }
            messages.remove(messages.first());
        } while ((messages.first().getNextId() != null) && (count != length));
        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SocketSimulator that = (SocketSimulator) o;

        if (port != that.port) return false;
        if (serverSocket != null ? !serverSocket.equals(that.serverSocket) : that.serverSocket != null) return false;
        return inetAddress != null ? inetAddress.equals(that.inetAddress) : that.inetAddress == null;
    }

    @Override
    public int hashCode() {
        int result = serverSocket != null ? serverSocket.hashCode() : 0;
        result = 31 * result + (inetAddress != null ? inetAddress.hashCode() : 0);
        result = 31 * result + port;
        return result;
    }
}
