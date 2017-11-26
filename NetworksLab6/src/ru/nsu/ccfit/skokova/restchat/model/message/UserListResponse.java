package ru.nsu.ccfit.skokova.restchat.model.message;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import ru.nsu.ccfit.skokova.restchat.model.server.ConnectedClient;

import java.util.ArrayList;

public class UserListResponse extends Message {
    @JsonProperty("users")
    private ArrayList<ConnectedClient> connectedClients;

    @JsonCreator
    public UserListResponse(@JsonProperty("users") ArrayList<ConnectedClient> connectedClients) {
        this.connectedClients = connectedClients;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < connectedClients.size(); i++) {
            stringBuilder.append(i + 1);
            stringBuilder.append(". ");
            stringBuilder.append(connectedClients.get(i).getUsername());
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }
}
