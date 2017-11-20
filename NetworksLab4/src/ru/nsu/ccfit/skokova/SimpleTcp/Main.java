package ru.nsu.ccfit.skokova.SimpleTcp;

import org.codehaus.jackson.map.ObjectMapper;
import ru.nsu.ccfit.skokova.SimpleTcp.message.ConnectMessage;
import ru.nsu.ccfit.skokova.SimpleTcp.message.DataMessage;
import ru.nsu.ccfit.skokova.SimpleTcp.message.Message;
import ru.nsu.ccfit.skokova.SimpleTcp.message.idGenerator.IdGenerator;
import ru.nsu.ccfit.skokova.SimpleTcp.server.SimpleTcpServerSocket;
import ru.nsu.ccfit.skokova.SimpleTcp.server.SocketSimulator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Main { //Sandbox
    public static void main(String[] args) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            IdGenerator idGenerator = new IdGenerator();
            /*
            Message message = new ConnectMessage("192.168.0.1", 4500);
            String messageString = objectMapper.writeValueAsString(message);
            System.out.println(messageString);
            Message message1 = objectMapper.readValue(messageString, Message.class);
            System.out.println(message1.getClass().getName());*/

            byte[] bytes = new byte[7];
            bytes[0] = 0;
            bytes[1] = 0;
            bytes[2] = 0;
            bytes[3] = 0;
            bytes[4] = 0;
            bytes[5] = 4;
            bytes[6] = 1;

            DataMessage message = new DataMessage(bytes);
            message.setHostName("127.0.0.`");
            message.setPort(3248);
            message.setId(idGenerator.newId());
            message.setNextId(idGenerator.newId());
            String messageString = objectMapper.writeValueAsString(message);
            System.out.println(messageString);
            byte[] messageBytes = objectMapper.writeValueAsBytes(message);
            Message message1 = objectMapper.readValue(messageBytes, Message.class);
            System.out.println(message1.getClass().getName());

            SimpleTcpServerSocket simpleTcpServerSocket = new SimpleTcpServerSocket(8595);

            Map<SocketSimulator, String> map = new HashMap<>();
            SocketSimulator socketSimulator = new SocketSimulator(simpleTcpServerSocket, "localhost", 2249);
            map.put(socketSimulator, "Hello");

            SocketSimulator simulator = new SocketSimulator(simpleTcpServerSocket, "localhost", 2249);
            String result = map.get(simulator);
            System.out.println(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
