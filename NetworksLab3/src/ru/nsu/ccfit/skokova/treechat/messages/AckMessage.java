package ru.nsu.ccfit.skokova.treechat.messages;

public class AckMessage implements Message {
    @Override
    public byte[] serialize() {
        return new byte[0];
    }

    @Override
    public AckMessage deserialize(byte[] bytes) {
        return null;
    }

    @Override
    public void process() {

    }
}
