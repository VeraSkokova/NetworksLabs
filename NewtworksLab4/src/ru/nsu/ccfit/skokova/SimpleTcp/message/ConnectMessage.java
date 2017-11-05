package ru.nsu.ccfit.skokova.SimpleTcp.message;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

@JsonPropertyOrder({"messageType", "hostName", "port"})
public class ConnectMessage extends Message {
    private String hostName;
    private int port;

    public ConnectMessage() {
        this.messageType = MessageType.CONNECT;
    }

    @JsonCreator
    public ConnectMessage(@JsonProperty("hostName") String hostName, @JsonProperty("port") int port) {
        this();
        this.hostName = hostName;
        this.port = port;
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
}
