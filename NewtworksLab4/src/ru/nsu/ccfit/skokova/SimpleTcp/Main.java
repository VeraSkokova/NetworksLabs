package ru.nsu.ccfit.skokova.SimpleTcp;

import org.codehaus.jackson.map.ObjectMapper;
import ru.nsu.ccfit.skokova.SimpleTcp.message.ConnectMessage;
import ru.nsu.ccfit.skokova.SimpleTcp.message.DataMessage;
import ru.nsu.ccfit.skokova.SimpleTcp.message.Message;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            /*
            Message message = new ConnectMessage("192.168.0.1", 4500);
            String messageString = objectMapper.writeValueAsString(message);
            System.out.println(messageString);
            Message message1 = objectMapper.readValue(messageString, Message.class);
            System.out.println(message1.getClass().getName());*/

            byte[] bytes = new byte[4];
            bytes[0] = 32;
            bytes[1] = 64;
            bytes[2] = 41;
            bytes[3] = 58;

            Message message = new DataMessage(bytes);
            String messageString = objectMapper.writeValueAsString(message);
            System.out.println(messageString);
            Message message1 = objectMapper.readValue(messageString, Message.class);
            System.out.println(message1.getClass().getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
