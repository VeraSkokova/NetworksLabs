package ru.nsu.ccfit.skokova.restchat.model.message;

import org.codehaus.jackson.annotate.JsonProperty;

public class LogoutResponse extends Message {
    @JsonProperty("message")
    private static final String message = "bye";
}
