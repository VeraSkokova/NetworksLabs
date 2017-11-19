package ru.nsu.ccfit.skokova.SimpleTcp.example;

import ru.nsu.ccfit.skokova.SimpleTcp.client.SimpleTcpClientSocket;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Client {
    private static final int BUFFER_SIZE = 1024;
    private static final int RESPONSE_SIZE = 8;
    private static String filePath;
    private static String serverName;
    private static int serverPort;
    private SimpleTcpClientSocket socket;

    public static void main(String[] args) {
        filePath = args[0];
        serverName = args[1];
        serverPort = Integer.parseInt(args[2]);
        Client client = new Client();
        client.start();
    }

    private void start() {
        try {
            socket = new SimpleTcpClientSocket();
            socket.connect(serverName, serverPort);
            System.out.println("Connected");

            sendFile();

            byte[] response = new byte[RESPONSE_SIZE];
            int totalResponseRead = 0;
            while (totalResponseRead < RESPONSE_SIZE) {
                totalResponseRead += socket.receive(response, totalResponseRead, RESPONSE_SIZE - totalResponseRead);
            }
            String message = new String(response, StandardCharsets.UTF_8);
            System.out.println(message);

            try {
                socket.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Error in connection to server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void sendFile() throws IOException {
        String fileName = filePath;
        File file = new File(fileName);
        long fileSize = file.length();
        System.out.println("Prepared file");
        socket.sendLong(fileSize);
        socket.sendInt(fileName.length());
        socket.send(fileName.getBytes());
        FileInputStream fileInputStream = new FileInputStream(file);
        int read;
        byte[] buffer = new byte[BUFFER_SIZE];
        while ((read = fileInputStream.read(buffer, 0, buffer.length)) > 0) {
            socket.send(buffer);
        }
        System.out.println("File was sent");
    }
}

