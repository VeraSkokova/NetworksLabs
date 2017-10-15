package ru.nsu.ccfit.skokova.treechat.messages;

public class NewParentMessage implements Message {
    @Override
    public byte[] serialize() {
        return new byte[0];
    }

    @Override
    public NewParentMessage deserialize(byte[] bytes) {
        return null;
    }
}
