package ru.nsu.ccfit.skokova.SimpleTcp.message.utils;

import org.codehaus.jackson.map.ObjectMapper;
import ru.nsu.ccfit.skokova.SimpleTcp.message.Message;

import java.io.IOException;
import java.util.Arrays;

public class MessageUtils {
    public static void fillMessageBytes(byte[] messageBytes, int start, byte[] data) {
        for (byte b : data) {
            messageBytes[start] = b;
            start++;
        }
    }

    public static byte[] createDataMessage(byte[] header, byte[] messageBytes) {
        byte[] msgBytes = new byte[1 + header.length + messageBytes.length];
        msgBytes[0] = (byte) header.length;
        fillMessageBytes(msgBytes, 1, header);
        fillMessageBytes(msgBytes, 1 + header.length, messageBytes);
        return msgBytes;
    }

    public static byte[] createServiceMessage(byte[] header) {
        byte[] msgBytes = new byte[1 + header.length];
        msgBytes[0] = (byte) header.length;
        fillMessageBytes(msgBytes, 1, header);
        return msgBytes;
    }

    public static Message getHeader(byte[] bytes) throws IOException {
        int headerLength = bytes[0];
        byte[] header = Arrays.copyOfRange(bytes, 1, bytes.length);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(header, Message.class);
    }

    public static byte[] getData(byte[] bytes, int headerLength, int dataLength) {
        return Arrays.copyOfRange(bytes, 1 + headerLength, 1 + headerLength + dataLength );
    }
}
