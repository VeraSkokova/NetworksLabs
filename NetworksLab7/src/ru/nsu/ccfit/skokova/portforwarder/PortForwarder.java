package ru.nsu.ccfit.skokova.portforwarder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PortForwarder {
    private static final int BUF_SIZE = 4096;
    private int lPort;
    private int rPort;
    private String rHost;
    private InetSocketAddress inetSocketAddress;
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;

    private Map<SocketChannel, SocketChannel> connectionMap = new HashMap<>();
    private Map<SocketChannel, ByteBuffer> bytesToSend = new HashMap<>();

    public PortForwarder(int lPort, int rPort, String rHost) {
        this.lPort = lPort;
        this.rPort = rPort;
        this.rHost = rHost;
    }

    public static void main(String[] args) {
        int lPort = Integer.parseInt(args[0]);
        String rHost = args[1];
        int rPort = Integer.parseInt(args[2]);

        PortForwarder portForwarder = new PortForwarder(lPort, rPort, rHost);
        portForwarder.start();
    }

    public void start() {
        try {
            resolveDns();

            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(lPort));
            serverSocketChannel.configureBlocking(false);

            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("Listening on port " + lPort);

            while (true) {
                int ready = selector.select();

                if (ready == 0) {
                    continue;
                }

                Iterator iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = (SelectionKey) iterator.next();
                    iterator.remove();
                    processRequest(key);
                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void resolveDns() throws UnknownHostException {
        InetAddress inetAddress = InetAddress.getByName(rHost);
        inetSocketAddress = new InetSocketAddress(inetAddress, rPort);
    }

    private void processRequest(SelectionKey selectionKey) throws IOException {
        if (selectionKey.isAcceptable()) {
            registerClient();
        } else if (selectionKey.isReadable()) {
            processInput(selectionKey);
        } else if (selectionKey.isWritable()) {
            processOutput(selectionKey);
        } else if (selectionKey.isConnectable()) {
            processConnect(selectionKey);
        }
    }

    private void registerClient() throws IOException {
        try {
            SocketChannel clientSocketChannel = serverSocketChannel.accept();
            clientSocketChannel.configureBlocking(false);
            System.out.println("New connection from local " + clientSocketChannel.getLocalAddress() + " remote " + clientSocketChannel.getRemoteAddress());

            SocketChannel connectionSocketChannel = SocketChannel.open();
            connectionSocketChannel.configureBlocking(false);
            boolean isConnected = connectionSocketChannel.connect(inetSocketAddress);
            if (!isConnected) {
                connectionSocketChannel.register(selector, SelectionKey.OP_CONNECT);
            }

            connectionMap.put(clientSocketChannel, connectionSocketChannel);
            connectionMap.put(connectionSocketChannel, clientSocketChannel);

            bytesToSend.put(clientSocketChannel, ByteBuffer.allocate(BUF_SIZE));
            bytesToSend.put(connectionSocketChannel, ByteBuffer.allocate(BUF_SIZE));
        } catch (NullPointerException e) {
            //System.out.println("null caught");
        }
    }

    private void processInput(SelectionKey selectionKey) throws IOException {
        try {
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
            System.out.println("New input from local " + socketChannel.getLocalAddress() + " remote " + socketChannel.getRemoteAddress());
            ByteBuffer buffer = bytesToSend.get(socketChannel);
            int read = socketChannel.read(buffer);
            SocketChannel anotherSocketChannel = connectionMap.get(socketChannel);
            buffer.flip();
            anotherSocketChannel.write(buffer);
            if (buffer.hasRemaining()) {
                socketChannel.register(selector, SelectionKey.OP_WRITE);
            }
            buffer.flip();
            System.out.println("Wrote data from input");
            if (read == -1) {
                socketChannel.shutdownOutput();
                System.out.println("Shutdown input");

            }
        } catch (ClosedChannelException e) {
            SocketChannel first = (SocketChannel) selectionKey.channel();
            SocketChannel second = (SocketChannel) selectionKey.channel();
            first.close();
            second.close();
            connectionMap.remove(first);
            connectionMap.remove(second);
            bytesToSend.remove(first);
            bytesToSend.remove(second);
        }
    }

    private void processOutput(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        System.out.println("New output from local " + socketChannel.getLocalAddress() + " remote " + socketChannel.getRemoteAddress());
        SocketChannel anotherSocketChannel = connectionMap.get(socketChannel);
        ByteBuffer buffer = bytesToSend.get(anotherSocketChannel);
        buffer.flip();
        socketChannel.write(buffer);
        if (!buffer.hasRemaining()) {
            selectionKey.cancel();
        }
        buffer.flip();
        System.out.println("Wrote data from output");
    }

    private void processConnect(SelectionKey selectionKey) throws IOException {
        SocketChannel connectionSocketChannel = (SocketChannel) selectionKey.channel();
        boolean isConnected = connectionSocketChannel.finishConnect();
        if (!isConnected) {
            System.out.println("Haven't connected yet");
            connectionSocketChannel.register(selector, SelectionKey.OP_CONNECT);
            return;
        }

        System.out.println("New output from remote " + connectionSocketChannel.getRemoteAddress());
        SocketChannel clientSocketChannel = connectionMap.get(connectionSocketChannel);

        clientSocketChannel.register(selector, SelectionKey.OP_READ);
        connectionSocketChannel.register(selector, SelectionKey.OP_READ);

    }
}
