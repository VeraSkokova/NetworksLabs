package ru.nsu.ccfit.skokova.restchat.model.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.org.apache.regexp.internal.RE;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import ru.nsu.ccfit.skokova.restchat.model.message.LoginRequest;
import ru.nsu.ccfit.skokova.restchat.model.message.LoginResponse;
import ru.nsu.ccfit.skokova.restchat.model.utils.ResponseCodes;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class LoginClientHandler implements HttpHandler {
    private static final Logger logger = LogManager.getLogger(Server.class);
    private ObjectMapper objectMapper = new ObjectMapper();
    private Server server;

    public LoginClientHandler(Server server) {
        this.server = server;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        System.out.println("Hi!");
        String requestMethod = httpExchange.getRequestMethod();
        Headers headers = httpExchange.getRequestHeaders();
        InputStream inputStream = httpExchange.getRequestBody();
        if (requestMethod.equalsIgnoreCase("POST")) {
            processLogin(headers, inputStream, httpExchange);
        } else {
            sendLoginError(httpExchange, ResponseCodes.METHOD_NOT_ALLOWED);
        }
    }

    private void processLogin(Headers headers, InputStream inputStream, HttpExchange httpExchange) {
        try {
            if (headers.get("Content-Type").get(0).equals("application/json")) {
                LoginRequest loginRequest = objectMapper.readValue(inputStream, LoginRequest.class);
                String username = loginRequest.getUsername();
                if (!server.getUsernames().contains(username)) {
                    logger.debug("New user: " + username);
                    server.getUsernames().add(username);
                    ConnectedClient connectedClient = new ConnectedClient(server.getNewId(), UUID.randomUUID(), username, true);
                    server.getConnectedClients().add(connectedClient);
                    sendLoginSuccess(connectedClient, httpExchange);
                } else {
                    sendLoginError(httpExchange, ResponseCodes.UNAUTHORIZED);
                }
            } else {
                sendLoginError(httpExchange, ResponseCodes.BAD_REQUEST);
            }
        } catch (IOException e) {
            //logger.error(e.getMessage());
            System.err.println(e.getMessage());
        }
    }

    private void sendLoginSuccess(ConnectedClient connectedClient, HttpExchange httpExchange) throws IOException {
        LoginResponse loginResponse = new LoginResponse(connectedClient.getId(), connectedClient.getUsername(), true, connectedClient.getToken());
        DataOutputStream dataOutputStream = new DataOutputStream(httpExchange.getResponseBody());
        String responseString = objectMapper.writeValueAsString(loginResponse);

        Headers responseHeaders = httpExchange.getResponseHeaders();
        responseHeaders.set("Content-type", "application/json");

        httpExchange.sendResponseHeaders(ResponseCodes.OK, responseString.getBytes().length);
        dataOutputStream.writeBytes(responseString);
        dataOutputStream.flush();
        dataOutputStream.close();
    }

    private void sendLoginError(HttpExchange httpExchange, int code) throws IOException {
        Headers responseHeaders = httpExchange.getResponseHeaders();
        if (code == ResponseCodes.UNAUTHORIZED) {
            responseHeaders.set("WWW-Authenticate", "Token realm=’Username is already in use");
        }
        httpExchange.sendResponseHeaders(code, -1);
    }
}