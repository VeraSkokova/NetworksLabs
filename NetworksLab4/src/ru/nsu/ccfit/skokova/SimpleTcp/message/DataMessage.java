package ru.nsu.ccfit.skokova.SimpleTcp.message;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

import java.util.UUID;

@JsonPropertyOrder({"messageType", "dataLength", "id", "nextId"})
public class DataMessage extends Message {
    private long nextId;
    @JsonProperty("dataLength")
    private int dataLength;

    public DataMessage() {
        this.messageType = MessageType.DATA;
    }

    @JsonCreator
    public DataMessage(String hostName, int port, int dataLength) {
        this();
        this.hostName = hostName;
        this.port = port;
        this.nextId = -1;
        this.dataLength = dataLength;
    }

    @JsonCreator
    public DataMessage(@JsonProperty("nextId") long nextId, String hostName, int port, int dataLength) {
        this();
        this.hostName = hostName;
        this.port = port;
        this.nextId = nextId;
        this.dataLength = dataLength;
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

    public int getDataLength() {
        return dataLength;
    }

    public void setDataLength(int dataLength) {
        this.dataLength = dataLength;
    }
}
