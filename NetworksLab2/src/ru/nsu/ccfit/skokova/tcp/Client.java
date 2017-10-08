package ru.nsu.ccfit.skokova.tcp;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Client {
    private static String filePath;
    private static String serverName;
    private static int serverPort;
    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    public static void main(String[] args) {
        filePath = args[0];
        serverName = args[1];
        serverPort = Integer.parseInt(args[2]);
        Client client = new Client();
        client.start();
    }

    private void start() {
        try {
            socket = new Socket(serverName, serverPort);

            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());

            sendFile();

            byte[] response = new byte[8];
            dataInputStream.readFully(response);
            String message = new String(response, StandardCharsets.UTF_8);
            System.out.println(message);

            try {
                socket.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("Error in connection to server: " + e.getMessage());
            System.exit(1);
        }
    }

    private void sendFile() throws IOException {
        String fileName = filePath;
        File file = new File(fileName);
        long fileSize = file.length();
        System.out.println("Prepared file");
        dataOutputStream.writeLong(fileSize);
        dataOutputStream.writeInt(fileName.length());
        dataOutputStream.writeBytes(fileName);
        FileInputStream fileInputStream = new FileInputStream(file);
        int read;
        byte[] buffer = new byte[1024];
        while ((read = fileInputStream.read(buffer, 0, buffer.length)) > 0) {
            dataOutputStream.write(buffer, 0, read);
            dataOutputStream.flush();
        }
        System.out.println("File was sent");
    }
}
