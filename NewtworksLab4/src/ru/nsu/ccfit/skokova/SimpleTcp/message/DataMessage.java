package ru.nsu.ccfit.skokova.SimpleTcp.message;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

@JsonPropertyOrder({"messageType", "data", "id"})
public class DataMessage extends Message {
    @JsonProperty("data")
    private byte[] data;

    public DataMessage() {
        this.messageType = MessageType.DATA;
    }

    @JsonCreator
    public DataMessage(@JsonProperty("data") byte[] data) {
        this();
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
