package ru.nsu.ccfit.skokova.SimpleTcp.message;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

import java.util.concurrent.atomic.AtomicInteger;

@JsonDeserialize(using = MessageDeserializer.class)
public abstract class Message implements Comparable<Message>{
    protected MessageType messageType;
    protected static final AtomicInteger ID = new AtomicInteger(1);
    @JsonProperty("id")
    protected int id;
    protected String hostName;
    protected int port;

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

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message = (Message) o;

        return id == message.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public int compareTo(Message message) {
        return id - message.getId();
    }
}
