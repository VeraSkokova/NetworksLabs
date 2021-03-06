package ru.nsu.ccfit.skokova.portforwarder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

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
    private Map<SocketChannel, Boolean> isClosableFromOutputMap = new HashMap<>();

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
            e.printStackTrace();
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
            } else {
                System.out.println("New output from remote " + connectionSocketChannel.getRemoteAddress());

                clientSocketChannel.register(selector, SelectionKey.OP_READ);
                connectionSocketChannel.register(selector, SelectionKey.OP_READ);
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
            SocketChannel anotherSocketChannel = connectionMap.get(socketChannel);
            Set<SelectionKey> keys = selector.keys();

            //System.out.println("New input from local " + socketChannel.getLocalAddress() + " remote " + socketChannel.getRemoteAddress());
            ByteBuffer buffer = bytesToSend.get(socketChannel);
            int read = socketChannel.read(buffer);
            System.out.println(socketChannel.getRemoteAddress() + " read " + read + " bytes");

            if (read == -1) {
                System.out.println("Can't read no more (in processInput remote: " + socketChannel.getRemoteAddress() + ")");
                pauseOption(selectionKey, SelectionKey.OP_READ);
                if (buffer.position() != 0) {
                    System.out.println("Something else " + buffer.position());
                    //buffer.flip();
                    for (SelectionKey key : keys) {
                        if (key.channel().equals(anotherSocketChannel)) {
                            resumeOption(key, SelectionKey.OP_WRITE);
                            isClosableFromOutputMap.put(anotherSocketChannel, true);
                        }
                    }
                } else {
                    anotherSocketChannel.shutdownOutput();
                    System.out.println("Shutdown output");
                }

                if ((bytesToSend.get(socketChannel).position() == 0) && (bytesToSend.get(anotherSocketChannel).position() == 0)) {
                    throw new ClosedChannelException();
                }
                if (isClosableFromOutputMap.containsKey(socketChannel)) {
                    throw new ClosedChannelException();
                }
                return;
            }

            //buffer.flip();
            for (SelectionKey key : keys) {
                if (key.channel().equals(anotherSocketChannel)) {
                    resumeOption(key, SelectionKey.OP_WRITE);
                }
            }
            pauseOption(selectionKey, SelectionKey.OP_READ);
        } catch (ClosedChannelException e) {
            System.out.println("Closing");
            SocketChannel first = (SocketChannel) selectionKey.channel();
            SocketChannel second = connectionMap.get(first);
            closeChannels(first, second);
            deleteChannels(first, second);
        }
    }

    private void processOutput(SelectionKey selectionKey) throws IOException {
        try {
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
            SocketChannel anotherSocketChannel = connectionMap.get(socketChannel);
            Set<SelectionKey> keys = selector.keys();

            //System.out.println("New output from local " + socketChannel.getLocalAddress() + " remote " + socketChannel.getRemoteAddress());
            ByteBuffer buffer = bytesToSend.get(anotherSocketChannel);
            buffer.flip();
            int write = socketChannel.write(buffer);
            System.out.println("Write " + write + " bytes to " + socketChannel.getRemoteAddress());
            if (!buffer.hasRemaining()) {
                System.out.println("Buffer is full (in processOutput)");
                pauseOption(selectionKey, SelectionKey.OP_WRITE);
            }
            buffer.compact();

            if (isClosableFromOutputMap.containsKey(socketChannel)) {
                if (bytesToSend.get(anotherSocketChannel).position() == 0) {
                    pauseOption(selectionKey, SelectionKey.OP_WRITE);
                    socketChannel.shutdownOutput();
                    if ((bytesToSend.get(socketChannel).position() == 0) && (isClosableFromOutputMap.containsKey(anotherSocketChannel))) {
                        throw new ClosedChannelException();
                    } else {
                        for (SelectionKey key : keys) {
                            if (key.channel().equals(anotherSocketChannel)) {
                                resumeOption(key, SelectionKey.OP_WRITE);
                                isClosableFromOutputMap.put(anotherSocketChannel, true);
                            }
                        }
                    }
                }
                //return;
            }
            for (SelectionKey key : keys) {
                if (key.channel().equals(anotherSocketChannel)) {
                    resumeOption(key, SelectionKey.OP_READ);
                }
            }
            //System.out.println("Wrote data from output");
        } catch (ClosedChannelException e) {
            System.out.println("Closing");
            SocketChannel first = (SocketChannel) selectionKey.channel();
            SocketChannel second = connectionMap.get(first);
            closeChannels(first, second);
            deleteChannels(first, second);
        }
    }

    private void processConnect(SelectionKey selectionKey) throws IOException {
        SocketChannel connectionSocketChannel = (SocketChannel) selectionKey.channel();
        SocketChannel clientSocketChannel = connectionMap.get(connectionSocketChannel);

        boolean isConnected = connectionSocketChannel.finishConnect();
        if (!isConnected) {
            System.out.println("Haven't connected yet");
            deleteChannels(connectionSocketChannel, clientSocketChannel);
            return;
        }

        System.out.println("Connect response from remote " + connectionSocketChannel.getRemoteAddress());

        clientSocketChannel.register(selector, SelectionKey.OP_READ);
        connectionSocketChannel.register(selector, SelectionKey.OP_READ);

    }

    private void deleteChannels(SocketChannel first, SocketChannel second) {
        System.out.println("Deleting channels");
        connectionMap.remove(first);
        connectionMap.remove(second);
        bytesToSend.remove(first);
        bytesToSend.remove(second);
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
        selectionKey.interestOps(selectionKey.interestOps() & ~option);
    }

    private void resumeOption(SelectionKey selectionKey, int option) {
        selectionKey.interestOps(selectionKey.interestOps() | option);
    }
}
