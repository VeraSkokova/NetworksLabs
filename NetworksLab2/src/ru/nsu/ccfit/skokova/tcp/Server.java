package ru.nsu.ccfit.skokova.tcp;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

public class Server {
    public static final String DIR_NAME = "./uploads";
    public static final int PERIOD = 3000;
    private static int serverPort;
    private Thread acceptor;

    public static void main(String[] args) {
        serverPort = Integer.parseInt(args[0]);
        Server server = new Server();
        server.start();
    }

    private void start() {
        File uploadsDirectory = new File(DIR_NAME);
        if (!uploadsDirectory.exists()) {
            uploadsDirectory.mkdir();
        }
        acceptor = new Thread(new AcceptorRunnable());
        acceptor.start();
    }

    class AcceptorRunnable implements Runnable {
        @Override
        public void run() {
            ServerSocket serverSocket;
            try {
                serverSocket = new ServerSocket(serverPort);
                while (!Thread.interrupted()) {
                    Socket socket;
                    try {
                        socket = serverSocket.accept();
                        System.out.println("New connection: " + socket.getInetAddress().getHostAddress());
                        ConnectedClient connectedClient = new ConnectedClient(socket);
                        connectedClient.run();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}


class ConnectedClient {
    private static final int BUF_SIZE = 1024;

    private Socket socket;

    ConnectedClient(Socket socket) {
        this.socket = socket;
    }

    void run() {
        Thread handler = new Thread(new Handler());
        handler.start();
    }

    class Handler implements Runnable {
        private long totalRead = 0;

        @Override
        public void run() {
            try (DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                 DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {
                System.out.println("Running handler");

                long fileSize = dataInputStream.readLong();
                System.out.println("FileSize: " + fileSize);
                int fileNameSize = dataInputStream.readInt();
                System.out.println("FileNameSize: " + fileNameSize);

                int read;
                byte[] stringBuffer = new byte[fileNameSize];
                int totalFileNameRead = 0;
                while (totalFileNameRead < fileNameSize) {
                    totalFileNameRead += dataInputStream.read(stringBuffer, totalFileNameRead, fileNameSize - totalFileNameRead);
                }
                String fileName = new String(stringBuffer, StandardCharsets.UTF_8);

                File tempFile = new File(fileName);
                String name = tempFile.getName();

                System.out.println("Got file " + name);
                File file = new File(Server.DIR_NAME + "/" + name);

                Timer timer = new Timer();
                timer.scheduleAtFixedRate(new SpeedMeter(this), 2, Server.PERIOD);

                try (OutputStream outputStream = new FileOutputStream(file)) {
                    byte[] buffer = new byte[BUF_SIZE];

                    synchronized (this) {
                        while ((totalRead < fileSize) && ((read = dataInputStream.read(buffer, 0, buffer.length)) != -1)) {
                            outputStream.write(buffer, 0, read);
                            totalRead += read;
                            outputStream.flush();
                        }
                    }

                    String response;

                    if (fileSize == totalRead) {
                        response = "Success!";
                        System.out.println("Wrote file to directory");
                    } else {
                        response = "Error!";
                        System.out.println("Problem with saving file");
                    }

                    dataOutputStream.writeBytes(response);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }

                timer.cancel();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }

        synchronized long getTotalRead() {
            return totalRead;
        }
    }
}

class SpeedMeter extends TimerTask {
    private long startTime;
    private long previousTime;
    private long previouslyRead;
    private ConnectedClient.Handler handler;

    SpeedMeter(ConnectedClient.Handler handler) {
        this.startTime = System.currentTimeMillis();
        this.handler = handler;
        this.previousTime = this.startTime;
    }

    @Override
    public void run() {
        long totalRead = handler.getTotalRead();
        if (totalRead != 0) {
            long currentTime = System.currentTimeMillis();
            double instantaneousSpeed = (1.0 * totalRead - previouslyRead) / (currentTime - previousTime);
            System.out.println("Instantaneous speed: " + instantaneousSpeed);
            double averageSpeed = 1.0 * totalRead / (currentTime - startTime);
            System.out.println("Average Speed: " + averageSpeed);
            previouslyRead = totalRead;
            previousTime = currentTime;
        }
    }
}

