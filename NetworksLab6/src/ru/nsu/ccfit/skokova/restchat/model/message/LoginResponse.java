package ru.nsu.ccfit.skokova.restchat.model.message;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.UUID;

public class LoginResponse extends Message {
    @JsonProperty("id")
    private int id;
    @JsonProperty("username")
    private String username;
    @JsonProperty("online")
    private boolean online;
    @JsonProperty("token")
    private UUID token;

    @JsonCreator
    public LoginResponse(@JsonProperty("id") int id, @JsonProperty("username") String username, @JsonProperty("online") boolean online, @JsonProperty("token") UUID token) {
        this.id = id;
        this.username = username;
        this.online = online;
        this.token = token;
    }

    public int getId() {
        return id;
    }

    public UUID getToken() {
        return token;
    }
}
