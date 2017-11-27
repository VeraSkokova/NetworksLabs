package ru.nsu.ccfit.skokova.restchat.model.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.codehaus.jackson.map.ObjectMapper;
import ru.nsu.ccfit.skokova.restchat.model.message.MessageHolder;
import ru.nsu.ccfit.skokova.restchat.model.message.MessageListResponse;
import ru.nsu.ccfit.skokova.restchat.model.message.MessageRequest;
import ru.nsu.ccfit.skokova.restchat.model.message.MessageResponse;
import ru.nsu.ccfit.skokova.restchat.model.utils.ResponseCodes;

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
        if (!checkRequest(httpExchange)) {
            return;
        }
        Headers headers = httpExchange.getRequestHeaders();
        UUID uuid;
        try {
            uuid = UUID.fromString(headers.get("Authorization").get(0));
        } catch (IllegalArgumentException e) {
            sendMessageError(httpExchange, ResponseCodes.FORBIDDEN);
            return;
        }
        ConnectedClient tempConnectedClient = new ConnectedClient(uuid);
        if (!server.getConnectedClients().contains(tempConnectedClient)) {
            sendMessageError(httpExchange, ResponseCodes.FORBIDDEN);
            return;
        }
        if (httpExchange.getRequestMethod().equalsIgnoreCase("POST")) {
            int clientIndex = server.getConnectedClients().indexOf(tempConnectedClient);
            ConnectedClient connectedClient = server.getConnectedClients().get(clientIndex);
            processNewMessage(httpExchange, connectedClient);
        } else if (httpExchange.getRequestMethod().equalsIgnoreCase("GET")) {
            processGetMessages(httpExchange);
        } else {
            sendMessageError(httpExchange, ResponseCodes.BAD_REQUEST);
        }
    }

    private void processNewMessage(HttpExchange httpExchange, ConnectedClient connectedClient) throws IOException {
        Headers headers = httpExchange.getRequestHeaders();
        if (headers.get("Content-Type").get(0).equals("application/json")) {
            //System.out.println("New message from " + connectedClient.getUsername());
            ObjectMapper objectMapper = new ObjectMapper();
            InputStream inputStream = httpExchange.getRequestBody();
            MessageRequest messageRequest = objectMapper.readValue(inputStream, MessageRequest.class);
            MessageHolder messageHolder = new MessageHolder(messageRequest.getMessage(), connectedClient.getId());
            //System.out.println("Message is " + messageHolder.getMessage());
            server.getMessages().add(messageHolder);
            messageHolder.setId(server.getMessages().indexOf(messageHolder));
            //System.out.println("Message id is " + messageHolder.getId());

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
            //System.out.println("Got GET request for " + count + " messages from " + offset);
            ArrayList<MessageHolder> messageHolders = new ArrayList<>();
            for (int i = offset; i < count + offset; i++) {
                if (i >= server.getMessages().size()) {
                    break;
                }
                messageHolders.add(server.getMessages().get(i));
                //System.out.println("Add " + i + " message to response body");
            }
            sendMessageListResponse(httpExchange, messageHolders);
        }

    }

    private void sendMessageResponse(HttpExchange httpExchange, MessageHolder messageHolder, ConnectedClient connectedClient) throws IOException {
        MessageResponse messageResponse = new MessageResponse(messageHolder.getId(), messageHolder.getMessage());
        ObjectMapper objectMapper = new ObjectMapper();
        String messageResponseString = objectMapper.writeValueAsString(messageResponse);
        //System.out.println("Send " + messageResponseString + " to " + connectedClient.getUsername());
        DataOutputStream dataOutputStream = new DataOutputStream(httpExchange.getResponseBody());

        Headers responseHeaders = httpExchange.getResponseHeaders();
        responseHeaders.set("Content-type", "application/json");

        httpExchange.sendResponseHeaders(ResponseCodes.OK, messageResponseString.getBytes().length);
        dataOutputStream.writeBytes(messageResponseString);
        dataOutputStream.flush();
        dataOutputStream.close();
    }

    private void sendMessageListResponse(HttpExchange httpExchange, ArrayList<MessageHolder> messageHolders) throws IOException {
        MessageListResponse messageListResponse = new MessageListResponse(messageHolders);
        ObjectMapper objectMapper = new ObjectMapper();
        String messageListResponseString = objectMapper.writeValueAsString(messageListResponse);
        //System.out.println("Sending " + messageListResponseString);
        DataOutputStream dataOutputStream = new DataOutputStream(httpExchange.getResponseBody());

        Headers responseHeaders = httpExchange.getResponseHeaders();
        responseHeaders.set("Content-type", "application/json");

        httpExchange.sendResponseHeaders(ResponseCodes.OK, messageListResponseString.getBytes().length);
        dataOutputStream.writeBytes(messageListResponseString);
        dataOutputStream.flush();
        dataOutputStream.close();
    }

    private void sendMessageError(HttpExchange httpExchange, int code) throws IOException {
        httpExchange.sendResponseHeaders(code, -1);
    }

    private boolean checkRequest(HttpExchange httpExchange) throws IOException {
        if (!httpExchange.getRequestHeaders().containsKey("Authorization")) {
            sendMessageError(httpExchange, ResponseCodes.BAD_REQUEST);
            return false;
        }
        return true;
    }
}
