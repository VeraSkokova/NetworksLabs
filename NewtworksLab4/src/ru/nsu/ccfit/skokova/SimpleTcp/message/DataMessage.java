package ru.nsu.ccfit.skokova.SimpleTcp.message;

public class DataMessage extends Message {
    public DataMessage() {
        this.messageType = MessageType.DATA;
    }
}
