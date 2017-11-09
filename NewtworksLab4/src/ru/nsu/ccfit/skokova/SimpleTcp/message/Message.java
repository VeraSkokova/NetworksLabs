package ru.nsu.ccfit.skokova.SimpleTcp.message;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

import java.util.concurrent.atomic.AtomicInteger;

@JsonDeserialize(using = MessageDeserializer.class)
public abstract class Message {
    protected MessageType messageType;
    protected static final AtomicInteger ID = new AtomicInteger(0);
    @JsonProperty("id")
    protected int id;

    @JsonCreator
    public Message() {
        this.id = ID.getAndIncrement();
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }
}
