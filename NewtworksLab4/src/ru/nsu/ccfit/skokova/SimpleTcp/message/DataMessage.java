package ru.nsu.ccfit.skokova.SimpleTcp.message;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

@JsonPropertyOrder({"messageType", "data", "id", "nextId"})
public class DataMessage extends Message {
    @JsonProperty("data")
    private byte[] data;
    private int nextId;

    public DataMessage() {
        this.messageType = MessageType.DATA;
    }

    @JsonCreator
    public DataMessage(@JsonProperty("data") byte[] data) {
        this();
        this.data = data;
    }

    @JsonCreator
    public DataMessage(@JsonProperty("data") byte[] data, @JsonProperty("nextId") int nextId) {
        this();
        this.data = data;
        this.nextId = nextId;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getNextId() {
        return nextId;
    }
}
