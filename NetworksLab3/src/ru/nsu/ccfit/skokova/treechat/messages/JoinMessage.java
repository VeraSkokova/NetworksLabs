package ru.nsu.ccfit.skokova.treechat.messages;

public class JoinMessage implements Message {
    @Override
    public byte[] serialize() {
        return new byte[0];
    }

    @Override
    public JoinMessage deserialize(byte[] bytes) {
        return null;
    }

    @Override
    public void process() {

    }
}
