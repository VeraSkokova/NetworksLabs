package ru.nsu.ccfit.skokova.restchat.model.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.codehaus.jackson.map.ObjectMapper;
import ru.nsu.ccfit.skokova.restchat.model.message.MessageHolder;
import ru.nsu.ccfit.skokova.restchat.model.message.MessageListResponse;
import ru.nsu.ccfit.skokova.restchat.model.message.MessageRequest;
import ru.nsu.ccfit.skokova.restchat.model.message.MessageResponse;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;

public class MessagesClientHandler implements HttpHandler {
    private Server server;

    public MessagesClientHandler(Server server) {
        this.server = server;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        System.out.println("Messages");
        Headers headers = httpExchange.getRequestHeaders();
        UUID uuid = UUID.fromString(headers.get("Authorization").get(0));
        ConnectedClient tempConnectedClient = new ConnectedClient(uuid);
        if (!server.getConnectedClients().contains(tempConnectedClient)) {
            sendMessageError(httpExchange, 403);
            return;
        }
        if (httpExchange.getRequestMethod().equalsIgnoreCase("POST")) {
            processNewMessage(httpExchange, tempConnectedClient);
        } else if (httpExchange.getRequestMethod().equalsIgnoreCase("GET")) {
            processGetMessages(httpExchange);
        } else {
            sendMessageError(httpExchange, 400);
        }
    }

    private void processNewMessage(HttpExchange httpExchange, ConnectedClient connectedClient) throws IOException {
        Headers headers = httpExchange.getRequestHeaders();
        if (headers.get("Content-Type").get(0).equals("application/json")) {
            ObjectMapper objectMapper = new ObjectMapper();
            InputStream inputStream = httpExchange.getRequestBody();
            MessageRequest messageRequest = objectMapper.readValue(inputStream, MessageRequest.class);
            MessageHolder messageHolder = new MessageHolder(messageRequest.getMessage(), connectedClient.getId());
            server.getMessages().add(messageHolder);
            messageHolder.setId(server.getMessages().indexOf(messageHolder));

            sendMessageResponse(httpExchange, messageHolder, connectedClient);
        }
    }

    private void processGetMessages(HttpExchange httpExchange) throws IOException {
        String query = httpExchange.getRequestURI().getQuery();
        String[] queryParts = query.split("&");
        if (queryParts.length == 2) {
            String[] offsetPart = queryParts[0].split("=");
            int offset = Integer.parseInt(offsetPart[1]);
            String[] countPart = queryParts[1].split("=");
            int count = Integer.parseInt(countPart[1]);
            ArrayList<MessageHolder> messageHolders = new ArrayList<>();
            for (int i = offset; i < count; i++) {
                if (offset >= server.getMessages().size()) {
                    break;
                }
                messageHolders.add(server.getMessages().get(i));
            }
            sendMessageListResponse(httpExchange, messageHolders);
        }

    }

    private void sendMessageResponse(HttpExchange httpExchange, MessageHolder messageHolder, ConnectedClient connectedClient) throws IOException {
        MessageResponse messageResponse = new MessageResponse(messageHolder.getId(), messageHolder.getMessage());
        ObjectMapper objectMapper = new ObjectMapper();
        String messageResponseString = objectMapper.writeValueAsString(messageResponse);
        DataOutputStream dataOutputStream = new DataOutputStream(httpExchange.getResponseBody());

        Headers responseHeaders = httpExchange.getResponseHeaders();
        responseHeaders.set("Content-type", "application/json");

        httpExchange.sendResponseHeaders(200, messageResponseString.getBytes().length);
        dataOutputStream.writeBytes(messageResponseString);
        dataOutputStream.flush();
        dataOutputStream.close();
    }

    private void sendMessageListResponse(HttpExchange httpExchange, ArrayList<MessageHolder> messageHolders) throws IOException {
        MessageListResponse messageListResponse = new MessageListResponse(messageHolders);
        ObjectMapper objectMapper = new ObjectMapper();
        String messageListResponseString = objectMapper.writeValueAsString(messageListResponse);
        DataOutputStream dataOutputStream = new DataOutputStream(httpExchange.getResponseBody());

        Headers responseHeaders = httpExchange.getResponseHeaders();
        responseHeaders.set("Content-type", "application/json");

        httpExchange.sendResponseHeaders(200, messageListResponseString.getBytes().length);
        dataOutputStream.writeBytes(messageListResponseString);
        dataOutputStream.flush();
        dataOutputStream.close();
    }

    private void sendMessageError(HttpExchange httpExchange, int code) throws IOException {
        httpExchange.sendResponseHeaders(code, -1);
    }
}
