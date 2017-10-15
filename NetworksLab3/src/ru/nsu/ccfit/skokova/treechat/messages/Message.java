package ru.nsu.ccfit.skokova.treechat.messages;

public interface Message {
    byte[] serialize();

    Message deserialize(byte[] bytes);
}
