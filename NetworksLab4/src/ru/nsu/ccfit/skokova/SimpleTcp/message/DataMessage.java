package ru.nsu.ccfit.skokova.SimpleTcp.message;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

import java.util.UUID;

@JsonPropertyOrder({"messageType", "data", "id", "nextId"})
public class DataMessage extends Message {
    @JsonProperty("data")
    private byte[] data;
    private UUID nextId;

    public DataMessage() {
        this.messageType = MessageType.DATA;
    }

    @JsonCreator
    public DataMessage(@JsonProperty("data") byte[] data) {
        this();
        this.data = data;
    }

    @JsonCreator
    public DataMessage(@JsonProperty("data") byte[] data, @JsonProperty("nextId") UUID nextId) {
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

    public UUID getNextId() {
        return nextId;
    }

    public void setNextId(UUID nextId) {
        this.nextId = nextId;
    }

    public void setId(UUID uuid) {
        this.id = uuid;
    }
}
