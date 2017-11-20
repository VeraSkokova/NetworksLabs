package ru.nsu.ccfit.skokova.SimpleTcp.message;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import ru.nsu.ccfit.skokova.SimpleTcp.message.serialization.MessageDeserializer;

import java.util.UUID;

@JsonDeserialize(using = MessageDeserializer.class)
public class Message implements Comparable<Message> {
    protected MessageType messageType;
    @JsonProperty("id")
    protected long id;
    protected String hostName;
    protected int port;
    protected long time;
    @JsonIgnore
    protected long lastAttempt = System.currentTimeMillis();

    @JsonCreator
    public Message() {
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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getLastAttempt() {
        return lastAttempt;
    }

    public void setLastAttempt(long lastAttempt) {
        this.lastAttempt = lastAttempt;
    }

    @Override
    public boolean equals(Object o) {
        /*if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;*/

        Message message = (Message) o;

        //if (time != message.time) return false;
        return id == message.id;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        //result = 31 * result + (int) (time ^ (time >>> 32));
        return result;
    }

    @Override
    public int compareTo(Message message) {
        /*long compareTime = time - message.time;
        if (compareTime != 0) {
            return (int) compareTime;
        }*/
        return (int) (id - message.id);
    }
}
