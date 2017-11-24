package ru.nsu.ccfit.skokova.restchat.model.server;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private static final Logger logger = LogManager.getLogger(Server.class);

    private static final int PORT = 2359;
    private static final String ADDRESS = "localhost";
    private static final String LOGIN_PATH = "/login";

    public static final int MIN_PORT_NUMBER = 0;
    public static final int MAX_PORT_NUMBER = 65535;

    private ArrayList<String> usernames = new ArrayList<>();
    private ArrayList<ConnectedClient> connectedClients = new ArrayList<>();

    private static final AtomicInteger ID = new AtomicInteger();

    public static void main(String[] args) {
        try {
            Server server = new Server();
            server.start();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void start() throws IOException {
        HttpServer httpServer = HttpServer.create();
        httpServer.bind(new InetSocketAddress(ADDRESS, PORT), 0);
        HttpContext loginHttpContext = httpServer.createContext(LOGIN_PATH, new LoginClientHandler(this));
        httpServer.start();
    }

    public ArrayList<String> getUsernames() {
        return usernames;
    }

    public int getNewId() {
        return ID.getAndIncrement();
    }
}
