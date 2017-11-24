package ru.nsu.ccfit.skokova.restchat.model.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import ru.nsu.ccfit.skokova.restchat.model.message.LoginRequest;
import ru.nsu.ccfit.skokova.restchat.model.message.LoginResponse;

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
        System.out.println(requestMethod);
        Headers headers = httpExchange.getRequestHeaders();
        for (String s : headers.keySet()) {
            System.out.println(s + " : " + headers.get(s));
        }
        InputStream inputStream = httpExchange.getRequestBody();
        if (requestMethod.equalsIgnoreCase("POST")) {
            processLogin(headers, inputStream, httpExchange);
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
                    sendLoginSuccess(username, httpExchange);
                }
            } else {
                System.err.println("Problem!!!");
            }
        } catch (IOException e) {
            //logger.error(e.getMessage());
            System.err.println(e.getMessage());
        }
    }

    private void sendLoginSuccess(String username, HttpExchange httpExchange) throws IOException {
        LoginResponse loginResponse = new LoginResponse(server.getNewId(), username, true, UUID.randomUUID());
        DataOutputStream dataOutputStream = new DataOutputStream(httpExchange.getResponseBody());
        String responseString = objectMapper.writeValueAsString(loginResponse);

        Headers responseHeaders = httpExchange.getResponseHeaders();
        responseHeaders.set("Content-type", "application/json");

        httpExchange.sendResponseHeaders(200, responseString.getBytes().length);
        dataOutputStream.writeBytes(responseString);
        dataOutputStream.flush();
        dataOutputStream.close();
    }
}