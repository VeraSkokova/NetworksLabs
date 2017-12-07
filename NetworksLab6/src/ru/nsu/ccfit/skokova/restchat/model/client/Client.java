package ru.nsu.ccfit.skokova.restchat.model.client;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import ru.nsu.ccfit.skokova.restchat.model.message.*;
import ru.nsu.ccfit.skokova.restchat.model.utils.ResponseCodes;
import ru.nsu.ccfit.skokova.restchat.view.ClientConnectedHandler;
import ru.nsu.ccfit.skokova.restchat.view.ClientFrame;
import ru.nsu.ccfit.skokova.restchat.view.ValueChangedHandler;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Client {
    public static final String CONTENT_TYPE = "application/json";
    private static final String PREFIX = "http://";
    private static final String USER_AGENT = "Mozilla/5.0";
    private static final int MESSAGES_COUNT = 10;
    private static final int SLEEP_TIME = 1000;
    private final Object lock = new Object();
    private Logger logger = LogManager.getLogger(Client.class);
    private String username;
    private boolean isLoggedIn;
    private String server;
    private int port;
    private ArrayList<ValueChangedHandler> handlers = new ArrayList<>();
    private ClientConnectedHandler clientConnectedHandler;
    private int myId;
    private UUID myToken;
    private int currentMessageId;
    private Map<Integer, String> users = new HashMap<>();

    public static void main(String[] args) {
        Client client = new Client();
        ClientFrame clientFrame = new ClientFrame(client);
        client.addHandler(clientFrame.new MessageUpdater());
    }

    public void start() {
        try {
            URL url = new URL("http", server, port, "/login");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", CONTENT_TYPE);
            connection.setRequestProperty("User-agent", USER_AGENT);
            connection.setRequestProperty("Host", server + ":" + port);
            connection.setRequestProperty("Accept-language", "en-US,en;q=0.5");

            LoginRequest loginRequest = new LoginRequest(username);
            ObjectMapper objectMapper = new ObjectMapper();
            String loginRequestString = objectMapper.writeValueAsString(loginRequest);

            connection.setDoOutput(true);
            connection.setUseCaches(false);
            DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
            dataOutputStream.writeBytes(loginRequestString);
            dataOutputStream.flush();
            dataOutputStream.close();

            int responseCode = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();
            if (responseCode == ResponseCodes.OK) {
                isLoggedIn = true;
                clientConnectedHandler.handle(true);
                LoginResponse loginResponse = objectMapper.readValue(connection.getInputStream(), LoginResponse.class);
                myId = loginResponse.getId();
                myToken = loginResponse.getToken();
                users.put(myId, username);
                synchronized (lock) {
                    applyForMessages();
                }
                Thread messageApplier = new Thread(new MessageApplier());
                messageApplier.start();
            } else {
                handleChange(responseMessage);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public void sendTextMessage(String message) {
        synchronized (lock) {
            try {
                URL url = new URL("http", server, port, "/messages");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                //System.out.println("Sending " + message);

                connection.setRequestMethod("POST");
                connection.setRequestProperty("Authorization", myToken.toString());
                connection.setRequestProperty("Content-Type", CONTENT_TYPE);
                connection.setRequestProperty("User-agent", USER_AGENT);

                MessageRequest messageRequest = new MessageRequest(message);
                ObjectMapper objectMapper = new ObjectMapper();
                String messageRequestString = objectMapper.writeValueAsString(messageRequest);

                connection.setDoOutput(true);
                DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
                dataOutputStream.writeBytes(messageRequestString);
                dataOutputStream.flush();
                dataOutputStream.close();

                int responseCode = connection.getResponseCode();
                if (responseCode == ResponseCodes.OK) {
                    //System.out.println(message + " delivered successfully");
                    MessageResponse messageResponse = objectMapper.readValue(connection.getInputStream(), MessageResponse.class);
                    if ((messageResponse.getId() - currentMessageId) > 1) {
                        applyForMessages();
                        //System.out.println("Need some more messages");
                    } else {
                        handleChange(username + " : " + messageResponse.getMessage());
                        //System.out.println("Printing");
                    }
                    currentMessageId = messageResponse.getId();
                    //System.out.println("Increased currentMessageId to " + currentMessageId);
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private void applyForMessages() {
        try {
            MessageListResponse messageListResponse = new MessageListResponse(new ArrayList<>());
            int offset = currentMessageId + 1;
            do {
                URL url = new URL("http", server, port, "/messages?offset=" + offset + "&count=" + MESSAGES_COUNT);
                //System.out.println("Asking for " + MESSAGES_COUNT + " messages from " + offset);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", myToken.toString());
                connection.setRequestProperty("Content-Type", CONTENT_TYPE);
                connection.setRequestProperty("User-agent", USER_AGENT);

                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode == ResponseCodes.OK) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    messageListResponse = objectMapper.readValue(connection.getInputStream(), MessageListResponse.class);
                    //System.out.println("Got " + messageListResponse.getMessages().size() + " new messages");
                } else {
                    break;
                }
                for (MessageHolder messageHolder : messageListResponse.getMessages()) {
                    String user = users.get(messageHolder.getAuthor());
                    if (user == null) {
                        sendUserInfoMessage(messageHolder.getAuthor());
                    }
                    handleChange(users.get(messageHolder.getAuthor()) + " : " + messageHolder.getMessage());
                    currentMessageId = messageListResponse.getMessages().get(messageListResponse.getMessages().size() - 1).getId();
                    //System.out.println("Increased currentMessageId to " + currentMessageId);
                }
                offset += MESSAGES_COUNT;
            } while (!messageListResponse.getMessages().isEmpty());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public void sendLogoutMessage() {
        try {
            URL url = new URL("http", server, port, "/logout");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-agent", USER_AGENT);
            connection.setRequestProperty("Authorization", myToken.toString());

            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode == ResponseCodes.OK) {
                ObjectMapper objectMapper = new ObjectMapper();
                LogoutResponse logoutResponse = objectMapper.readValue(connection.getInputStream(), LogoutResponse.class);
                handleChange(logoutResponse);
                clientConnectedHandler.handle(false);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public void sendUserListMessage() {
        try {
            URL url = new URL("http", server, port, "/users");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-agent", USER_AGENT);
            connection.setRequestProperty("Authorization", myToken.toString());

            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode == ResponseCodes.OK) {
                ObjectMapper objectMapper = new ObjectMapper();
                UserListResponse userListResponse = objectMapper.readValue(connection.getInputStream(), UserListResponse.class);
                handleChange(userListResponse);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

    }

    private void sendUserInfoMessage(int userId) {
        try {
            URL url = new URL("http", server, port, "/users/" + userId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-agent", USER_AGENT);
            connection.setRequestProperty("Authorization", myToken.toString());

            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode == ResponseCodes.OK) {
                ObjectMapper objectMapper = new ObjectMapper();
                UserResponse userResponse = objectMapper.readValue(connection.getInputStream(), UserResponse.class);
                users.put(userResponse.getId(), userResponse.getUsername());
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void handleChange(Object change) {
        for (ValueChangedHandler handler : handlers) {
            handler.handle(change);
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public ClientConnectedHandler getClientConnectedHandler() {
        return clientConnectedHandler;
    }

    public void setClientConnectedHandler(ClientConnectedHandler clientConnectedHandler) {
        this.clientConnectedHandler = clientConnectedHandler;
    }

    public void addHandler(ValueChangedHandler handler) {
        if (handler != null) {
            handlers.add(handler);
        }
    }

    class MessageApplier implements Runnable {
        @Override
        public void run() {
            try {
                int i = 0;
                while (true) {
                    synchronized (lock) {
                        applyForMessages();
                        //System.out.println("Gonna get new messages " + i);
                    }
                    i++;
                    Thread.sleep(SLEEP_TIME);
                }
            } catch (InterruptedException e) {
                System.err.println("Interrupted");
            }
        }
    }
}

