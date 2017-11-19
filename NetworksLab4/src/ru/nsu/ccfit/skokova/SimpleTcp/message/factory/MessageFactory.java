package ru.nsu.ccfit.skokova.SimpleTcp.message.factory;

import org.codehaus.jackson.JsonParser;
import ru.nsu.ccfit.skokova.SimpleTcp.message.Message;
import ru.nsu.ccfit.skokova.SimpleTcp.message.MessageType;

import java.util.HashMap;
import java.util.Map;

public class MessageFactory {
    private final static Map<String, Class> MESSAGE_CREATORS = new HashMap<>();

    static {
        MESSAGE_CREATORS.put(MessageType.CONNECT.name(), ConnectMessageCreator.class);
        MESSAGE_CREATORS.put(MessageType.DATA.name(), DataMessageCreator.class);
        MESSAGE_CREATORS.put(MessageType.DISCONNECT.name(), DisconnectMessageCreator.class);
        MESSAGE_CREATORS.put(MessageType.ACK.name(), AckMessageCreator.class);
    }

    public static Message createMessage(String name, JsonParser jsonParser) throws IllegalAccessException, InstantiationException {
        MessageCreator messageCreator = (MessageCreator) MESSAGE_CREATORS.get(name).newInstance();
        return messageCreator.createMessage(jsonParser);
    }
}
