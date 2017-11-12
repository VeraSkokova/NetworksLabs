package ru.nsu.ccfit.skokova.md5cracker.client;

import ru.nsu.ccfit.skokova.md5cracker.utils.Hex;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class Client {
    private static final int CODES_COUNT = 4;
    private static final char[] CODES = {'A', 'C', 'G', 'T'};
    private static final int TIME_TO_PRINT = 10;
    private static String serverName;
    private static int serverPort;
    private final UUID uuid;
    private long length;
    private long start;
    private long end;
    private String instruction;
    private boolean success;
    private String hash;
    private String result;

    public Client() {
        this.uuid = UUID.randomUUID();
        this.success = false;
    }

    public static void main(String[] args) {
        serverName = args[0];
        serverPort = Integer.parseInt(args[1]);
        Client client = new Client();
        client.start();
    }

    private void start() {
        while (true) {
            boolean done = getInfoFromServer();
            if (done) {
                if (instruction.equals("WORK")) {
                    calculateHashes();
                } else {
                    break;
                }
                if (success) {
                    sendSuccess(result);
                    break;
                }
            } else {
                try {
                    System.out.println("Server is unreachable, wait...");
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    System.out.println("Interrupted");
                }
            }
        }
    }

    private void calculateHashes() {
        try {
            MessageDigest md5Counter = MessageDigest.getInstance("MD5");
            for (long i = start; i < end; i++) {
                String currentString = codeToString(i, length);
                if (i % TIME_TO_PRINT == 0) {
                    System.err.println("Current string: " + currentString);
                }
                byte[] tempHash = md5Counter.digest(currentString.getBytes());
                String hexTempHash = Hex.toHexString(tempHash);
                if (hexTempHash.equals(hash)) {
                    System.err.println("Found solution: " + currentString);
                    result = currentString;
                    success = true;
                    return;
                }
            }
        } catch (NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
        }
    }

    private boolean getInfoFromServer() {
        try (Socket socket = new Socket(serverName, serverPort);
             DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
             DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {

            dataOutputStream.writeInt(uuid.toString().length());
            System.out.println("Wrote uuidSize: " + uuid.toString().length());
            dataOutputStream.writeUTF(uuid.toString());
            System.out.println("Wrote uuid: " + uuid.toString());

            if (hash == null) {
                System.out.println("First connection");
                int hashSize = dataInputStream.readInt();
                System.out.println("Read hashSize: " + hashSize);
                hash = dataInputStream.readUTF();
                System.out.println("Read hash: " + hash);
            } else {
                sendError(dataOutputStream);
            }

            instruction = dataInputStream.readUTF();
            System.out.println("Read instruction: " + instruction);

            if (instruction.equals("WORK")) {
                length = dataInputStream.readLong();
                start = dataInputStream.readLong();
                end = dataInputStream.readLong();
            } else if (instruction.equals("STOP")) {
                System.err.println("Stop finding solution");
            }
            return true;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    private void sendSuccess(String result) {
        try (Socket socket = new Socket(serverName, serverPort);
             DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {
            dataOutputStream.writeInt(uuid.toString().length());
            dataOutputStream.writeUTF(uuid.toString());
            dataOutputStream.writeUTF("SUCCESS");
            dataOutputStream.writeUTF(result);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void sendError(DataOutputStream dataOutputStream) {
        try {
            dataOutputStream.writeUTF("ERROR");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private String codeToString(long code, long length) {
        StringBuilder string = new StringBuilder();

        for (long i = 0; i < length; i++) {
            string.append(CODES[(int) (code % CODES_COUNT)]);
            code /= CODES_COUNT;
        }

        return String.valueOf(string.reverse());
    }
}
