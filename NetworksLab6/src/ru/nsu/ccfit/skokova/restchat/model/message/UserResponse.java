package ru.nsu.ccfit.skokova.restchat.model.message;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

public class UserResponse {
    @JsonProperty("id")
    private int id;
    @JsonProperty("username")
    private String username;
    @JsonProperty("online")
    private boolean online;

    @JsonCreator
    public UserResponse(@JsonProperty("id") int id, @JsonProperty("username") String username, @JsonProperty("online") boolean online) {
        this.id = id;
        this.username = username;
        this.online = online;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
}
