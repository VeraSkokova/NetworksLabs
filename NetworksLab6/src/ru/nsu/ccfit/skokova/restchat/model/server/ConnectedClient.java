package ru.nsu.ccfit.skokova.restchat.model.server;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.UUID;

public class ConnectedClient {
    @JsonProperty("id")
    private int id;
    @JsonIgnore
    private UUID token;
    @JsonProperty("username")
    private String username;
    @JsonProperty("online")
    private boolean online;

    @JsonCreator
    public ConnectedClient(@JsonProperty("id") int id, @JsonProperty("username") String username, @JsonProperty("online") boolean online) {
        this.id = id;
        this.username = username;
        this.online = online;
    }

    public ConnectedClient(int id, UUID token, String username, boolean online) {
        this.id = id;
        this.token = token;
        this.username = username;
        this.online = online;
    }

    public ConnectedClient(UUID token) {
        this.token = token;
    }

    public UUID getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public boolean isOnline() {
        return online;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConnectedClient that = (ConnectedClient) o;

        return token != null ? token.equals(that.token) : that.token == null;
    }

    @Override
    public int hashCode() {
        return token != null ? token.hashCode() : 0;
    }
}
