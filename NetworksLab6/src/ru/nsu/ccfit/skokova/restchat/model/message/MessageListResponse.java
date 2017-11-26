package ru.nsu.ccfit.skokova.restchat.model.message;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.ArrayList;

public class MessageListResponse extends Message {
    @JsonProperty("messages")
    private ArrayList<MessageHolder> messages;

    @JsonCreator
    public MessageListResponse(@JsonProperty("messages") ArrayList<MessageHolder> messages) {
        this.messages = messages;
    }

    public ArrayList<MessageHolder> getMessages() {
        return messages;
    }
}
