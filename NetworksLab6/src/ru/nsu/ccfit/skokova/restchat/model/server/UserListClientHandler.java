package ru.nsu.ccfit.skokova.restchat.model.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.codehaus.jackson.map.ObjectMapper;
import ru.nsu.ccfit.skokova.restchat.model.message.UserListResponse;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public class UserListClientHandler implements HttpHandler {
    private Server server;

    public UserListClientHandler(Server server) {
        this.server = server;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        try {
            System.out.println("UserList");
            Headers headers = httpExchange.getRequestHeaders();
            UUID uuid = UUID.fromString(headers.get("Authorization").get(0));
            ConnectedClient tempConnectedClient = new ConnectedClient(uuid);
            if (server.getConnectedClients().contains(tempConnectedClient)) {
                sendUsersSuccess(httpExchange);
            } else {
                sendUsersError(httpExchange);
            }
        } catch (IllegalArgumentException e) {
            sendUsersError(httpExchange);
        }
    }

    private void sendUsersSuccess(HttpExchange httpExchange) throws IOException {
        UserListResponse userListResponse = new UserListResponse(server.getConnectedClients());
        ObjectMapper objectMapper = new ObjectMapper();
        String userListResponseString = objectMapper.writeValueAsString(userListResponse);
        DataOutputStream dataOutputStream = new DataOutputStream(httpExchange.getResponseBody());

        Headers responseHeaders = httpExchange.getResponseHeaders();
        responseHeaders.set("Content-type", "application/json");

        httpExchange.sendResponseHeaders(200, userListResponseString.getBytes().length);
        dataOutputStream.writeBytes(userListResponseString);
        dataOutputStream.flush();
        dataOutputStream.close();
    }

    private void sendUsersError(HttpExchange httpExchange) throws IOException {
        Headers responseHeaders = httpExchange.getResponseHeaders();
        responseHeaders.set("WWW-Authenticate", "Token realm=’Bad token");
        httpExchange.sendResponseHeaders(403, -1);
    }
}
