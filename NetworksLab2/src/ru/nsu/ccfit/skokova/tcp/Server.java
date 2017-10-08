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

    private boolean isWorking;

    public static void main(String[] args) {
        serverPort = Integer.parseInt(args[0]);
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in))) {
            Server server = new Server();
            server.start();
            while (bufferedReader.readLine().compareTo("stop") != 0) ;
            server.stop();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void start() {
        isWorking = true;
        File uploadsDirectory = new File(DIR_NAME);
        if (!uploadsDirectory.exists()) {
            uploadsDirectory.mkdir();
        }
        acceptor = new Thread(new AcceptorRunnable());
        acceptor.start();
    }

    private void stop() {
        isWorking = false;
    }

    class AcceptorRunnable implements Runnable {
        @Override
        public void run() {
            ServerSocket serverSocket = null;
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
        private int totalRead = 0;

        @Override
        public void run() {
            try (DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                 DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {
                System.out.println("Running handler");

                long fileSize = dataInputStream.readLong();
                System.out.println("FileSize: " + fileSize);
                int fileNameSize = dataInputStream.readInt();
                System.out.println("FileNameSize: " + fileNameSize);

                int read = 0;
                byte[] stringBuffer = new byte[fileNameSize];
                dataInputStream.read(stringBuffer, 0, fileNameSize);
                String fileName = new String(stringBuffer, StandardCharsets.UTF_8);

                File tempFile = new File(fileName);
                String name = tempFile.getName();

                System.out.println("Got file " + name);
                File file = new File(Server.DIR_NAME + "/" + name);

                Timer timer = new Timer();
                timer.scheduleAtFixedRate(new SpeedMeter(this), 2, Server.PERIOD);

                try (OutputStream outputStream = new FileOutputStream(file)) {
                    byte[] buffer = new byte[BUF_SIZE];

                    while ((totalRead < fileSize) && ((read = dataInputStream.read(buffer, 0, buffer.length)) != -1)) {
                        outputStream.write(buffer, 0, read);
                        totalRead += read;
                        outputStream.flush();
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

        int getTotalRead() {
            return totalRead;
        }
    }
}

class SpeedMeter extends TimerTask {
    private long startTime;
    private int previouslyRead;
    private ConnectedClient.Handler handler;

    SpeedMeter(ConnectedClient.Handler handler) {
        this.startTime = System.currentTimeMillis();
        this.handler = handler;
    }

    @Override
    public void run() {
        int totalRead = handler.getTotalRead();
        if (totalRead != 0) {
            long currentTime = System.currentTimeMillis();
            double instantaneousSpeed = (1.0 * totalRead - previouslyRead) / (currentTime - startTime);
            System.out.println("Instantaneous speed: " + instantaneousSpeed);
            double averageSpeed = 1.0 * totalRead / (currentTime - startTime);
            System.out.println("Average Speed: " + averageSpeed);
            previouslyRead = totalRead;
        }
    }
}

