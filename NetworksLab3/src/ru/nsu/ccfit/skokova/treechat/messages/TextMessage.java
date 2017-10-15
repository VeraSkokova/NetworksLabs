package ru.nsu.ccfit.skokova.treechat.messages;

public class TextMessage implements Message {
    @Override
    public byte[] serialize() {
        return new byte[0];
    }

    @Override
    public TextMessage deserialize(byte[] bytes) {
        return null;
    }

    @Override
    public void process() {

    }
}
