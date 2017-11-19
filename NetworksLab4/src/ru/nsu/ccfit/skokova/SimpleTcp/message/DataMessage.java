package ru.nsu.ccfit.skokova.SimpleTcp.message;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

import java.util.UUID;

@JsonPropertyOrder({"messageType", "data", "id", "nextId"})
public class DataMessage extends Message {
    @JsonProperty("dataLength")
    private byte[] data;
    private long nextId;

    public DataMessage() {
        this.messageType = MessageType.DATA;
    }

    @JsonCreator
    public DataMessage(@JsonProperty("data") byte[] data, String hostName, int port) {
        this();
        this.hostName = hostName;
        this.port = port;
        this.data = data;
        this.nextId = -1;
    }

    @JsonCreator
    public DataMessage(@JsonProperty("data") byte[] data, @JsonProperty("nextId") long nextId, String hostName, int port) {
        this();
        this.hostName = hostName;
        this.port = port;
        this.data = data;
        this.nextId = nextId;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public long getNextId() {
        return nextId;
    }

    public void setNextId(long nextId) {
        this.nextId = nextId;
    }

    public void setId(long uuid) {
        this.id = uuid;
    }
}
