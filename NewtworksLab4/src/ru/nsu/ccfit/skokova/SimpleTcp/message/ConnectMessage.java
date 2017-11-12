package ru.nsu.ccfit.skokova.SimpleTcp.message;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

@JsonPropertyOrder({"messageType", "hostName", "port", "id"})
public class ConnectMessage extends Message {
    public ConnectMessage() {
        super();
        this.messageType = MessageType.CONNECT;
    }

    @JsonCreator
    public ConnectMessage(@JsonProperty("hostName") String hostName, @JsonProperty("port") int port) {
        this();
        this.hostName = hostName;
        this.port = port;
    }
}
