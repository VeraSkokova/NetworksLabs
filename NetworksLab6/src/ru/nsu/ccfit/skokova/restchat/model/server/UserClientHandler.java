package ru.nsu.ccfit.skokova.restchat.model.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.codehaus.jackson.map.ObjectMapper;
import ru.nsu.ccfit.skokova.restchat.model.message.UserResponse;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public class UserClientHandler implements HttpHandler {
    private static final int INDEX_BEGINNING = 7;
    private Server server;

    public UserClientHandler(Server server) {
        this.server = server;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        try {
            System.out.println("User");
            Headers headers = httpExchange.getRequestHeaders();
            UUID uuid = UUID.fromString(headers.get("Authorization").get(0));
            ConnectedClient tempConnectedClient = new ConnectedClient(uuid);
            if (server.getConnectedClients().contains(tempConnectedClient)) {
                String query = httpExchange.getRequestURI().getRawPath();
                String clientNumberString = query.substring(INDEX_BEGINNING);
                int clientNumber = Integer.parseInt(clientNumberString);
                ConnectedClient connectedClient = server.searchClient(clientNumber);
                if (connectedClient != null) {
                    sendUserSuccess(httpExchange, connectedClient);
                } else {
                    sendUserError(httpExchange, 404);
                }
            } else {
                sendUserError(httpExchange, 403);
            }
        } catch (IllegalArgumentException e) {
            sendUserError( httpExchange, 403);
        }
    }

    private void sendUserSuccess(HttpExchange httpExchange, ConnectedClient connectedClient) throws IOException {
        UserResponse userResponse = new UserResponse(connectedClient.getId(), connectedClient.getUsername(), connectedClient.isOnline());
        ObjectMapper objectMapper = new ObjectMapper();
        String userResponseString = objectMapper.writeValueAsString(userResponse);
        DataOutputStream dataOutputStream = new DataOutputStream(httpExchange.getResponseBody());

        Headers responseHeaders = httpExchange.getResponseHeaders();
        responseHeaders.set("Content-type", "application/json");

        httpExchange.sendResponseHeaders(200, userResponseString.getBytes().length);
        dataOutputStream.writeBytes(userResponseString);
        dataOutputStream.flush();
        dataOutputStream.close();
    }

    private void sendUserError(HttpExchange httpExchange, int code) throws IOException {
        httpExchange.sendResponseHeaders(code, -1);
    }
}
