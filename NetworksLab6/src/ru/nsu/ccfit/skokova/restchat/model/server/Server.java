package ru.nsu.ccfit.skokova.restchat.model.server;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ru.nsu.ccfit.skokova.restchat.model.message.MessageHolder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    public static final int MIN_PORT_NUMBER = 0;
    public static final int MAX_PORT_NUMBER = 65535;
    private static final int SLEEP_TIME = 3000;
    private static final int IS_OVER = 5000;

    private static final Logger logger = LogManager.getLogger(Server.class);

    private static final int PORT = 2359;
    private static final String ADDRESS = "localhost";

    private static final String LOGIN_PATH = "/login";
    private static final String USERLIST_PATH = "/users";
    private static final String LOGOUT_PATH = "/logout";
    private static final String USER_PATH = "/users/";
    private static final String MESSAGES_PATH = "/messages";

    private static final AtomicInteger ID = new AtomicInteger();
    private ArrayList<String> usernames = new ArrayList<>();
    private ArrayList<ConnectedClient> connectedClients = new ArrayList<>();
    private Map<ConnectedClient, Long> clientsLastConneced = Collections.synchronizedMap(new HashMap<>());
    private CopyOnWriteArrayList<MessageHolder> messages = new CopyOnWriteArrayList<>();

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
        HttpContext usersHttpContext = httpServer.createContext(USERLIST_PATH, new UserListClientHandler(this));
        HttpContext logoutHttpContext = httpServer.createContext(LOGOUT_PATH, new LogoutClientHandler(this));
        HttpContext userHttpContext = httpServer.createContext(USER_PATH, new UserClientHandler(this));
        HttpContext messageHttpContext = httpServer.createContext(MESSAGES_PATH, new MessagesClientHandler(this));
        httpServer.start();

        Thread timeChecker = new Thread(new TimeCheckerRunnable());
        timeChecker.start();
    }

    public ArrayList<String> getUsernames() {
        return usernames;
    }

    public ArrayList<ConnectedClient> getConnectedClients() {
        return connectedClients;
    }

    public int getNewId() {
        return ID.getAndIncrement();
    }

    public CopyOnWriteArrayList<MessageHolder> getMessages() {
        return messages;
    }

    public ConnectedClient searchClient(int id) {
        for (ConnectedClient connectedClient : connectedClients) {
            if (connectedClient.getId() == id) {
                return connectedClient;
            }
        }
        return null;
    }

    public ConnectedClient searchClient(String username) {
        for (ConnectedClient connectedClient : connectedClients) {
            if (connectedClient.getUsername().equals(username)) {
                return connectedClient;
            }
        }
        return null;
    }

    public void updateClientLastConnected(ConnectedClient connectedClient) {
        clientsLastConneced.put(connectedClient, System.currentTimeMillis());
        int index = connectedClients.indexOf(connectedClient);
        if (!connectedClients.get(index).isOnline()) {
            connectedClients.get(index).setOnline(true);
        }
    }

    class TimeCheckerRunnable implements Runnable {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(SLEEP_TIME);
                    long now = System.currentTimeMillis();
                    for (Iterator<ConnectedClient> iterator = clientsLastConneced.keySet().iterator(); iterator.hasNext(); ) {
                        ConnectedClient connectedClient = iterator.next();
                        if (now - clientsLastConneced.get(connectedClient) > IS_OVER) {
                            clientsLastConneced.remove(connectedClient);
                            int index = connectedClients.indexOf(connectedClient);
                            if (index != -1) {
                                connectedClients.get(index).setOnline(false);
                            }
                            System.out.println(connectedClient.getUsername() + " timed out");
                        }
                    }
                } catch (InterruptedException e) {
                    System.err.println("Interrupted");
                }
            }
        }
    }
}
