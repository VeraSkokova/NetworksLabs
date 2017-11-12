package ru.nsu.ccfit.skokova.md5cracker.server;

import ru.nsu.ccfit.skokova.md5cracker.task.Task;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Server {
    private static final int CODES_COUNT = 4;
    private static final int MAX_LENGTH = 8;
    private static final long MAX_TIME_DIFF = 10000;
    private static final long SLEEP_TIME = 3000;

    private static int serverPort;
    private static String hashString;
    private Map<String, Task> taskMap;
    private Map<String, Long> timeMap;
    private BlockingQueue<Task> tasks;

    private String result;

    public static void main(String[] args) {
        hashString = args[0];
        serverPort = Integer.parseInt(args[1]);
        Server server = new Server();
        server.start();
    }


    private void start() {
        try {
            taskMap = Collections.synchronizedMap(new HashMap<>());
            timeMap = Collections.synchronizedMap(new HashMap<>());
            tasks = new ArrayBlockingQueue<>((int) Math.pow(CODES_COUNT, MAX_LENGTH));
            fillTasks();
            Thread acceptor = new Thread(new AcceptorRunnable());
            acceptor.start();
            Thread timeChecker = new Thread(new TimeCheckerRunnable());
            timeChecker.start();
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        }
    }

    private void fillTasks() throws InterruptedException {
        int tasksSize = (int) Math.pow(CODES_COUNT, MAX_LENGTH);
        for (int i = 0; i < tasksSize; i++) {
            int length = i / CODES_COUNT + 1;
            int start = length * (i % CODES_COUNT);
            int end = start + (int) Math.pow(CODES_COUNT, length) / CODES_COUNT;
            tasks.put(new Task(length, start, end));
        }
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

    class TimeCheckerRunnable implements Runnable {
        @Override
        public void run() {
            try {
                Thread.sleep(SLEEP_TIME);
                long currentTime = System.currentTimeMillis();
                for (String key : timeMap.keySet()) {
                    if (timeMap.get(key) - currentTime < MAX_TIME_DIFF) {
                        Task task = taskMap.get(key);
                        tasks.put(task);
                        timeMap.remove(key);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    class ConnectedClient {
        private final Socket socket;

        ConnectedClient(Socket socket) {
            this.socket = socket;
        }

        void run() {
            Thread handler = new Thread(new Handler());
            handler.start();
        }

        class Handler implements Runnable {
            @Override
            public void run() {
                try (DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                     DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {

                    int uuidSize = dataInputStream.readInt();
                    System.out.println("Read uuidSize: " + uuidSize);
                    String uuidString = dataInputStream.readUTF();
                    System.out.println("Read uuid: " + uuidString);

                    if (!taskMap.containsKey(uuidString)) {
                        System.out.println("New client");
                        dataOutputStream.writeInt(hashString.length());
                        System.out.println("Wrote hashLength: " + hashString.length());
                        dataOutputStream.writeUTF(hashString);
                        System.out.println("Wrote hash: " + hashString);
                    } else {
                        System.out.println("Old client");
                        String message = dataInputStream.readUTF();
                        System.out.println("Read message: " + message);
                        if (message.equals("SUCCESS")) {
                            result = dataInputStream.readUTF();
                            System.err.println("Solution: " + result);
                            return;
                        } else if (message.equals("ERROR")) {
                            System.err.println(uuidString + " didn't find the solution");
                        }
                    }

                    if ((result == null) && (!tasks.isEmpty())) {
                        Task task = tasks.take();
                        taskMap.put(uuidString, task);
                        timeMap.put(uuidString, System.currentTimeMillis());
                        dataOutputStream.writeUTF("WORK");
                        dataOutputStream.writeLong(task.getLength());
                        dataOutputStream.writeLong(task.getStart());
                        dataOutputStream.writeLong(task.getEnd());
                    } else {
                        dataOutputStream.writeUTF("STOP");
                    }

                } catch (IOException e) {
                    System.out.println(e.getMessage());
                } catch (InterruptedException e) {
                    System.out.println("Interrupted");
                }
            }
        }
    }
}



