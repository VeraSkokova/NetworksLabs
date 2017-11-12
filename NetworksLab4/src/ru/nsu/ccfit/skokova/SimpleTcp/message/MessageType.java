package ru.nsu.ccfit.skokova.SimpleTcp.message;

import org.codehaus.jackson.annotate.JsonProperty;

public enum MessageType {
    @JsonProperty("CONNECT")
    CONNECT,
    @JsonProperty("DATA")
    DATA,
    @JsonProperty("DISCONNECT")
    DISCONNECT
}
