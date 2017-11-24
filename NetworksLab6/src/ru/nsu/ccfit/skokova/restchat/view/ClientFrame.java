package ru.nsu.ccfit.skokova.restchat.view;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ru.nsu.ccfit.skokova.restchat.model.client.Client;
import ru.nsu.ccfit.skokova.restchat.model.server.Server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ClientFrame extends JFrame {
    private static final Logger logger = LogManager.getLogger(Client.class);

    private Client client;

    private JTextField serverField;
    private JTextField portField;
    private JTextField usernameField;
    private JTextField messageField;
    private JTextArea messageArea;

    private JButton loginButton;
    private JButton logoutButton;
    private JButton usersButton;

    private JPanel connectionPanel;

    public ClientFrame(Client client) {
        super("Chat Client");
        this.client = client;
        createAndShowGUI();
        setButtons();
        setConnectionInfo();
        setMessageField();
        addClientConnectedHandler();
    }

    private void setConnectionInfo() {
        this.serverField.addActionListener(new ServerListener());
        this.portField.addActionListener(new PortListener());
        this.usernameField.addActionListener(new UsernameListener());
    }

    private void setButtons() {
        this.loginButton.addActionListener(actionEvent -> {
            if (!client.isLoggedIn() && isClientReady()) {
                try {
                    client.setServer(serverField.getText());
                    checkPortNumber();
                    client.setUsername(usernameField.getText());
                    messageArea.setText("");
                    client.start();
                } catch (NumberFormatException e) {
                    portField.setForeground(Color.RED);
                    logger.error("Invalid port number");
                }
            } else {
                logger.warn("Bad request");
            }
        });

        this.logoutButton.addActionListener(actionEvent -> {
            if (client.isLoggedIn()) {
                client.sendLogoutMessage();
            } else {
                logger.warn("Bad request");
            }
        });

        this.usersButton.addActionListener(actionEvent -> {
            if (client.isLoggedIn()) {
                client.sendUserListMessage();
            } else {
                logger.warn("Bad request");
            }
        });
    }

    private void setMessageField() {
        this.messageField.addActionListener(actionEvent -> {
            client.sendTextMessage(messageField.getText());
            messageField.setText("");
        });
    }

    private void addClientConnectedHandler() {
        client.setClientConnectedHandler(isConnected -> {
            if (isConnected) {
                loginButton.setEnabled(false);
                logoutButton.setEnabled(true);
                usersButton.setEnabled(true);
                serverField.setEditable(false);
                portField.setEditable(false);
                usernameField.setEditable(false);
                messageField.setEditable(true);
            } else {
                loginButton.setEnabled(true);
                logoutButton.setEnabled(false);
                usersButton.setEnabled(false);
                usernameField.setEditable(true);
                messageField.setEditable(false);
            }
        });
    }

    private void createAndShowGUI() {
        addConnectionInfo();
        addLoginInfo();
        addChatPanel();
        addSouthPanel();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(600, 600);
        setVisible(true);
    }

    private void addConnectionInfo() {
        this.connectionPanel = new JPanel(new GridLayout(3, 1));
        JPanel serverAndPortPanel = new JPanel(new GridLayout(1, 5, 1, 3));
        this.serverField = new JTextField("localhost");
        this.portField = new JTextField(client.getPort());
        this.serverField.setHorizontalAlignment(SwingConstants.LEFT);
        this.portField.setHorizontalAlignment(SwingConstants.RIGHT);
        serverAndPortPanel.add(this.serverField);
        serverAndPortPanel.add(this.portField);
        this.connectionPanel.add(serverAndPortPanel);
        this.add(this.connectionPanel, BorderLayout.NORTH);
    }

    private void addLoginInfo() {
        JLabel usernameLabel = new JLabel();
        usernameLabel.setText("Username");
        this.usernameField = new JTextField();
        this.connectionPanel.add(usernameLabel);
        this.connectionPanel.add(this.usernameField);
    }

    private void addChatPanel() {
        this.messageArea = new JTextArea("Welcome to the Chat\n");
        JScrollPane messageScrollPane = new JScrollPane(this.messageArea);
        JPanel chatPanel = new JPanel(new GridLayout(1, 1));
        chatPanel.add(messageScrollPane);
        this.messageArea.setEditable(false);
        this.add(chatPanel, BorderLayout.CENTER);
    }

    private void addSouthPanel() {
        this.loginButton = new JButton("Login");
        this.loginButton.setBackground(new Color(32, 50, 100));
        this.loginButton.setForeground(Color.WHITE);
        this.logoutButton = new JButton("Logout");
        this.logoutButton.setForeground(Color.WHITE);
        this.logoutButton.setBackground(new Color(32, 50, 100));
        this.logoutButton.setEnabled(false);
        this.usersButton = new JButton("UserList");
        this.usersButton.setForeground(Color.WHITE);
        this.usersButton.setBackground(new Color(32, 50, 100));
        this.usersButton.setEnabled(false);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(this.loginButton);
        buttonPanel.add(this.usersButton);
        buttonPanel.add(this.logoutButton);

        JPanel messagePanel = new JPanel(new GridLayout(1, 2, 1, 3));
        JLabel messageLabel = new JLabel("Send your message");
        this.messageField = new JTextField();
        this.messageField.setEditable(false);
        messagePanel.add(messageLabel);
        messagePanel.add(this.messageField);

        JPanel southPanel = new JPanel(new GridLayout(2, 1));
        southPanel.add(messagePanel);
        southPanel.add(buttonPanel);

        this.add(southPanel, BorderLayout.SOUTH);
    }

    public void dispose() {
        if (logoutButton.isEnabled()) {
            logoutButton.doClick();
        }
        super.dispose();
        System.exit(0);
    }

    private boolean isClientReady() {
        boolean result = true;
        if (serverField.getText().isEmpty()) {
            serverField.setText("Enter server name");
            result = false;
        }
        if (portField.getText().isEmpty()) {
            portField.setText("Enter port number");
            result = false;
        }
        if (usernameField.getText().isEmpty()) {
            usernameField.setText("Enter username");
            result = false;
        }
        return result;
    }

    private void checkPortNumber() throws NumberFormatException {
        int value = Integer.parseInt(portField.getText());
        if ((value < Server.MIN_PORT_NUMBER) || (value > Server.MAX_PORT_NUMBER)) {
            throw new NumberFormatException("Incorrect value");
        }
        client.setPort(value);
    }

    class PortListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            try {
                if (portField.getText().isEmpty()) {
                    portField.setText("Enter port number");
                } else {
                    if (!client.isLoggedIn()) {
                        checkPortNumber();
                    } else {
                        logger.warn("Bad request");
                    }
                }
            } catch (NumberFormatException e) {
                portField.setForeground(Color.RED);
                logger.error("Wrong field value");
            }
        }
    }

    class ServerListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            try {
                if (!client.isLoggedIn()) {
                    if (serverField.getText().isEmpty()) {
                        serverField.setText("Enter server address");
                    } else {
                        String value = serverField.getText();
                        client.setServer(value);
                    }
                } else {
                    logger.warn("Bad request");
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
    }

    class UsernameListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            try {
                if (usernameField.getText().isEmpty()) {
                    usernameField.setText("Enter your username");
                } else {
                    String value = usernameField.getText();
                    client.setUsername(value);
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
    }

    public class MessageUpdater implements ValueChangedHandler {
        @Override
        public void handle(Object value) {
            if (value != null) {
                messageArea.append(value.toString() + "\n");
                messageArea.setCaretPosition(messageArea.getText().length() - 1);
            }

        }
    }

}
