package ru.nsu.ccfit.skokova.SimpleTcp.server;

import ru.nsu.ccfit.skokova.SimpleTcp.message.DataMessage;

public class DataWrapper implements Comparable<DataWrapper> {
    private DataMessage dataMessage;
    private byte[] data;

    public DataWrapper(DataMessage dataMessage, byte[] data) {
        this.dataMessage = dataMessage;
        this.data = data;
    }

    public DataMessage getDataMessage() {
        return dataMessage;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public int compareTo(DataWrapper dataWrapper) {
        return (int) (dataMessage.getId() - dataWrapper.getDataMessage().getId());
    }
}
