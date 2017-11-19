package ru.nsu.ccfit.skokova.SimpleTcp.message;

public class AckMessage extends Message {
    private long ackId;

    public AckMessage(long ackId) {
        this.messageType = MessageType.ACK;
        this.ackId = ackId;
        this.id = -1;
    }

    public long getAckId() {
        return ackId;
    }

    public void setAckId(long ackId) {
        this.ackId = ackId;
    }
}
