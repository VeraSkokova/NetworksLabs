package ru.nsu.ccfit.skokova.SimpleTcp.server;

import org.apache.commons.lang.ArrayUtils;
import org.codehaus.jackson.map.ObjectMapper;
import ru.nsu.ccfit.skokova.SimpleTcp.message.DataMessage;
import ru.nsu.ccfit.skokova.SimpleTcp.message.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeSet;

public class SocketSimulator {
    private SimpleTcpServerSocket serverSocket;
    private String inetAddress;
    private int port;

    private TreeSet<DataMessage> messages = new TreeSet<>();

    public SocketSimulator(SimpleTcpServerSocket serverSocket, String inetAddress, int port) {
        this.serverSocket = serverSocket;
        this.inetAddress = inetAddress;
        this.port = port;
    }

    public void send(byte[] data) throws IOException {
        Message message = new DataMessage(data);
        message.setHostName(inetAddress);
        message.setPort(port);
        ObjectMapper objectMapper = new ObjectMapper();
        byte[] messageBytes = objectMapper.writeValueAsBytes(message);
        serverSocket.send(messageBytes);
    }

    public byte[] receive() {
        if (messages.isEmpty()) {
            messages = serverSocket.receive(this);
        }
        ArrayList<Byte> result = new ArrayList<>();
        do {
            byte[] bytes = messages.first().getData();
            for (byte b : bytes) {
                result.add(b);
            }
            messages.remove(messages.first());
        } while (messages.first().getNextId() != 0);
        return ArrayUtils.toPrimitive(result.toArray(new Byte[result.size()]));
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
