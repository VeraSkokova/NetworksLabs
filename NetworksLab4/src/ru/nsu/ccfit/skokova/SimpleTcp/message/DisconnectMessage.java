package ru.nsu.ccfit.skokova.SimpleTcp.message;

public class DisconnectMessage extends Message {
    public DisconnectMessage() {
        this.messageType = MessageType.DISCONNECT;
        this.id = -1;
    }
}
