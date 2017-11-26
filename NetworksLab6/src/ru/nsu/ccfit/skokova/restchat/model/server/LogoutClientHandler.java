package ru.nsu.ccfit.skokova.restchat.model.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.codehaus.jackson.map.ObjectMapper;
import ru.nsu.ccfit.skokova.restchat.model.message.LogoutResponse;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public class LogoutClientHandler implements HttpHandler {
    private Server server;

    public LogoutClientHandler(Server server) {
        this.server = server;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        try {
            System.out.println("Logout");
            Headers headers = httpExchange.getRequestHeaders();
            UUID uuid = UUID.fromString(headers.get("Authorization").get(0));
            ConnectedClient tempConnectedClient = new ConnectedClient(uuid);
            if (server.getConnectedClients().contains(tempConnectedClient)) {
                sendLogoutSuccess(httpExchange);
                int clientIndex = server.getConnectedClients().indexOf(tempConnectedClient);
                ConnectedClient connectedClient = server.getConnectedClients().get(clientIndex);
                server.getUsernames().remove(connectedClient.getUsername());
                server.getConnectedClients().remove(connectedClient);
            } else {
                sendLogoutError(httpExchange);
            }
        } catch (IllegalArgumentException e) {
            sendLogoutError(httpExchange);
        }
    }

    private void sendLogoutSuccess(HttpExchange httpExchange) throws IOException {
        LogoutResponse logoutResponse = new LogoutResponse("bye");
        ObjectMapper objectMapper = new ObjectMapper();
        String logoutResponseString = objectMapper.writeValueAsString(logoutResponse);
        DataOutputStream dataOutputStream = new DataOutputStream(httpExchange.getResponseBody());

        Headers responseHeaders = httpExchange.getResponseHeaders();
        responseHeaders.set("Content-type", "application/json");

        httpExchange.sendResponseHeaders(200, logoutResponseString.getBytes().length);
        dataOutputStream.writeBytes(logoutResponseString);
        dataOutputStream.flush();
        dataOutputStream.close();
    }

    private void sendLogoutError(HttpExchange httpExchange) throws IOException {
        Headers responseHeaders = httpExchange.getResponseHeaders();
        responseHeaders.set("WWW-Authenticate", "Token realm=’Bad token");
        httpExchange.sendResponseHeaders(403, -1);
    }
}
