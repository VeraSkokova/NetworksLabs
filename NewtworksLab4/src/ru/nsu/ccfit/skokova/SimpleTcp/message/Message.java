package ru.nsu.ccfit.skokova.SimpleTcp.message;

import org.codehaus.jackson.map.annotate.JsonDeserialize;

@JsonDeserialize(using = MessageDeserializer.class)
public abstract class Message {
    protected MessageType messageType;

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }
}
