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
import java.util.Set;

public class OldForwarder {
    private static final int BUF_SIZE = 4096;
    private int lPort;
    private int rPort;
    private String rHost;
    private InetSocketAddress inetSocketAddress;
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;

    private Map<SocketChannel, SocketChannel> connectionMap = new HashMap<>();
    private Map<SocketChannel, ByteBuffer> bytesToSend = new HashMap<>();
    private Map<SocketChannel, ConnectInfo> connectInfoMap = new HashMap<>();


    public OldForwarder(int lPort, int rPort, String rHost) {
        this.lPort = lPort;
        this.rPort = rPort;
        this.rHost = rHost;
    }

    public static void main(String[] args) {
        int lPort = Integer.parseInt(args[0]);
        String rHost = args[1];
        int rPort = Integer.parseInt(args[2]);

        OldForwarder portForwarder = new OldForwarder(lPort, rPort, rHost);
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

            connectInfoMap.put(clientSocketChannel, new ConnectInfo());
            connectInfoMap.put(connectionSocketChannel, new ConnectInfo());
        } catch (NullPointerException e) {
            //System.out.println("null caught");
        }
    }

    private void processInput(SelectionKey selectionKey) throws IOException {
        try {
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
            SocketChannel anotherSocketChannel = connectionMap.get(socketChannel);
            Set<SelectionKey> keys = selector.keys();

            //System.out.println("New input from local " + socketChannel.getLocalAddress() + " remote " + socketChannel.getRemoteAddress());
            ByteBuffer buffer = bytesToSend.get(socketChannel);

            if (!buffer.hasRemaining()) {
                pauseOption(selectionKey, SelectionKey.OP_READ);
                for (SelectionKey key : keys) {
                    if (key.channel().equals(anotherSocketChannel)) {
                        resumeOption(key, SelectionKey.OP_READ);
                    }
                }
                return;
            }

            int read = socketChannel.read(buffer);
            if (read != 0) {
                System.out.println(socketChannel.getRemoteAddress() + " read " + read + " bytes");
            }
            buffer.flip();
            int write = anotherSocketChannel.write(buffer);
            if (write != 0) {
                System.out.println("Write " + write + " bytes to " + anotherSocketChannel.getRemoteAddress());
            }
            buffer.compact();
            if (!buffer.hasRemaining()) {
                pauseOption(selectionKey, SelectionKey.OP_READ);
                for (SelectionKey key : keys) {
                    if (key.channel().equals(anotherSocketChannel)) {
                        resumeOption(key, SelectionKey.OP_READ);
                    }
                }
            }
            //System.out.println("Wrote data from input");
            if (read == -1) {
                socketChannel.shutdownInput();
                anotherSocketChannel.shutdownInput();
                connectInfoMap.get(socketChannel).setClosed();
                if (connectInfoMap.get(anotherSocketChannel).isClosed()) {
                    deleteChannels(socketChannel, anotherSocketChannel);
                    closeChannels(socketChannel, anotherSocketChannel);
                    System.out.println("Closed connection");
                }
                pauseOption(selectionKey, SelectionKey.OP_READ);
                connectInfoMap.get(socketChannel).setReadable(false);
                System.out.println("Shutdown");
                for (SelectionKey key : keys) {
                    if (key.channel().equals(anotherSocketChannel)) {
                        System.out.println("Resuming read in other connection");
                        resumeOption(key, SelectionKey.OP_READ);
                        selector.wakeup();
                    }
                }

            }
        } catch (ClosedChannelException e) {
            System.out.println("Another channel closed");
        }
    }

    private void processOutput(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        System.out.println("New output from local " + socketChannel.getLocalAddress() + " remote " + socketChannel.getRemoteAddress());
        SocketChannel anotherSocketChannel = connectionMap.get(socketChannel);
        ByteBuffer buffer = bytesToSend.get(anotherSocketChannel);
        buffer.flip();
        int write = socketChannel.write(buffer);
        if (write != 0) {
            System.out.println("Write " + write + " bytes to " + socketChannel.getRemoteAddress());
        }
        if (!buffer.hasRemaining()) {
            //selectionKey.cancel();
            pauseOption(selectionKey, SelectionKey.OP_WRITE);
        }
        buffer.compact();
        System.out.println("Wrote data from output");
    }

    private void processConnect(SelectionKey selectionKey) throws IOException {
        SocketChannel connectionSocketChannel = (SocketChannel) selectionKey.channel();
        boolean isConnected = connectionSocketChannel.finishConnect();
        if (!isConnected) {
            System.out.println("Haven't connected yet");
            return;
        }

        System.out.println("New connection response from remote " + connectionSocketChannel.getRemoteAddress());
        SocketChannel clientSocketChannel = connectionMap.get(connectionSocketChannel);

        clientSocketChannel.register(selector, SelectionKey.OP_READ);
        connectionSocketChannel.register(selector, SelectionKey.OP_READ);

    }

    private void deleteChannels(SocketChannel first, SocketChannel second) {
        System.out.println("Deleting channels");
        connectionMap.remove(first);
        connectionMap.remove(second);
        bytesToSend.remove(first);
        bytesToSend.remove(second);
        connectInfoMap.remove(first);
        connectInfoMap.remove(second);
    }

    private void closeChannels(SocketChannel first, SocketChannel second) {
        System.out.println("Closing channels");
        try {
            first.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        try {
            second.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void pauseOption(SelectionKey selectionKey, int option) {
        if (selectionKey.isValid()) {
            selectionKey.interestOps(selectionKey.interestOps() & ~option);
        }
    }

    private void resumeOption(SelectionKey selectionKey, int option) {
        if (selectionKey.isValid()) {
            selectionKey.interestOps(selectionKey.interestOps() | option);
        }
    }
}

class ConnectInfo {
    private boolean isReadable;
    private boolean isWritable;

    public ConnectInfo() {
        this.isReadable = true;
        this.isWritable = true;
    }

    public boolean isReadable() {
        return isReadable;
    }

    public void setReadable(boolean readable) {
        isReadable = readable;
    }

    public boolean isWritable() {
        return isWritable;
    }

    public void setWritable(boolean writable) {
        isWritable = writable;
    }

    public boolean isClosed() {
        return !isWritable && !isReadable;
    }

    public void setClosed() {
        this.isReadable = false;
        this.isWritable = false;
    }
}