package ru.nsu.ccfit.skokova.restchat.model.message;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

public class MessageRequest extends Message {
    @JsonProperty("message")
    private String message;

    @JsonCreator
    public MessageRequest(@JsonProperty("message") String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
