package ru.nsu.ccfit.skokova.restchat.model.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.codehaus.jackson.map.ObjectMapper;
import ru.nsu.ccfit.skokova.restchat.model.message.UserResponse;
import ru.nsu.ccfit.skokova.restchat.model.utils.ResponseCodes;

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
            if (!checkRequest(httpExchange)) {
                return;
            }
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
                    server.updateClientLastConnected(connectedClient);
                } else {
                    sendUserError(httpExchange, ResponseCodes.NOT_FOUND);
                }
            } else {
                sendUserError(httpExchange, ResponseCodes.FORBIDDEN);
            }
        } catch (IllegalArgumentException e) {
            sendUserError( httpExchange, ResponseCodes.FORBIDDEN);
        }
    }

    private void sendUserSuccess(HttpExchange httpExchange, ConnectedClient connectedClient) throws IOException {
        UserResponse userResponse = new UserResponse(connectedClient.getId(), connectedClient.getUsername(), connectedClient.isOnline());
        ObjectMapper objectMapper = new ObjectMapper();
        String userResponseString = objectMapper.writeValueAsString(userResponse);
        DataOutputStream dataOutputStream = new DataOutputStream(httpExchange.getResponseBody());

        Headers responseHeaders = httpExchange.getResponseHeaders();
        responseHeaders.set("Content-type", "application/json");

        httpExchange.sendResponseHeaders(ResponseCodes.OK, userResponseString.getBytes().length);
        dataOutputStream.writeBytes(userResponseString);
        dataOutputStream.flush();
        dataOutputStream.close();
    }

    private void sendUserError(HttpExchange httpExchange, int code) throws IOException {
        httpExchange.sendResponseHeaders(code, -1);
    }

    private boolean checkRequest(HttpExchange httpExchange) throws IOException {
        if (!httpExchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendUserError(httpExchange, ResponseCodes.METHOD_NOT_ALLOWED);
            return false;
        }
        if (!httpExchange.getRequestHeaders().containsKey("Authorization")) {
            sendUserError(httpExchange, ResponseCodes.BAD_REQUEST);
            return false;
        }
        return true;
    }
}
