package ru.nsu.ccfit.skokova.restchat.model.client;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import ru.nsu.ccfit.skokova.restchat.model.message.LoginRequest;
import ru.nsu.ccfit.skokova.restchat.view.ClientConnectedHandler;
import ru.nsu.ccfit.skokova.restchat.view.ClientFrame;
import ru.nsu.ccfit.skokova.restchat.view.ValueChangedHandler;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class Client {
    private Logger logger = LogManager.getLogger(Client.class);

    private String username;
    private boolean isLoggedIn;

    private String server;
    private int port;

    private static final String PREFIX = "http://";
    private static final String USER_AGENT = "Mozilla/5.0";
    public static final String CONTENT_TYPE = "application/json";

    private ArrayList<ValueChangedHandler> handlers = new ArrayList<>();
    private ClientConnectedHandler clientConnectedHandler;

    public static void main(String[] args) {
        Client client = new Client();
        ClientFrame clientFrame = new ClientFrame(client);
        client.addHandler(clientFrame.new MessageUpdater());
    }

    public void start() {
        try {
            String urlString = PREFIX + server + ":" + port + "/login";
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", CONTENT_TYPE);
            connection.setRequestProperty("User-agent" , USER_AGENT);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendTextMessage(String message) {

    }

    public void sendLogoutMessage() {

    }

    public void sendUserListMessage() {

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

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
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
}
