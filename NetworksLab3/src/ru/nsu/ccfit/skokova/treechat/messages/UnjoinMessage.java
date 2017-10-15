package ru.nsu.ccfit.skokova.treechat.messages;

public class UnjoinMessage implements Message {
    @Override
    public byte[] serialize() {
        return new byte[0];
    }

    @Override
    public UnjoinMessage deserialize(byte[] bytes) {
        return null;
    }
}
