package ru.nsu.ccfit.skokova.proxy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Proxy {
    private static final int BUF_SIZE = 48 * 1024;
    private int lPort;
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;

    private Map<SocketChannel, SocketChannel> connectionMap = new HashMap<>();
    private Map<SocketChannel, ByteBuffer> bytesToSend = new HashMap<>();

    private RequestHandler requestHandler;

    public Proxy(int lPort) {
        this.lPort = lPort;
    }

    public static void main(String[] args) {
        int lPort = Integer.parseInt(args[0]);

        Proxy proxy = new Proxy(lPort);
        proxy.start();
    }

    public void start() {
        try {
            requestHandler = new RequestHandler(this);

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

    public void connectChannel(SocketChannel socketChannel, InetSocketAddress inetSocketAddress, Connection connection) throws IOException {
        SocketChannel connectionSocketChannel = SocketChannel.open();
        connectionSocketChannel.configureBlocking(false);
        boolean isConnected = connectionSocketChannel.connect(inetSocketAddress);
        if (!isConnected) {
            SelectionKey selectionKey = connectionSocketChannel.register(selector, SelectionKey.OP_CONNECT);
            selectionKey.attach(new ConnectionWrapper(connection, false, socketChannel.keyFor(selector)));
        } else {
            SelectionKey selectionKey = connectionSocketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            selectionKey.attach(new ConnectionWrapper(connection, false, socketChannel.keyFor(selector)));
        }

        connectionMap.put(socketChannel, connectionSocketChannel);
        connectionMap.put(connectionSocketChannel, socketChannel);
    }

    private InetSocketAddress resolveDns(String rHost, int rPort) throws UnknownHostException {
        InetAddress inetAddress = InetAddress.getByName(rHost);
        return new InetSocketAddress(inetAddress, rPort);
    }

    private void processRequest(SelectionKey selectionKey) throws IOException {
        if (selectionKey.isAcceptable()) {
            registerClient();
        } else if (selectionKey.isConnectable()) {
            processConnect(selectionKey);
        } else {
            requestHandler.handleRequest(selectionKey);
        }
    }

    private void registerClient() throws IOException {
        try {
            SocketChannel clientSocketChannel = serverSocketChannel.accept();
            clientSocketChannel.configureBlocking(false);
            System.out.println("New connection from local " + clientSocketChannel.getLocalAddress() + " remote " + clientSocketChannel.getRemoteAddress());

            SelectionKey selectionKey = clientSocketChannel.register(selector, SelectionKey.OP_READ);
            selectionKey.attach(new ConnectionWrapper(new Connection(), true, null));
        } catch (NullPointerException e) {
            //System.out.println("null caught");
        }
    }

    private void processConnect(SelectionKey selectionKey) throws IOException {
        SocketChannel connectionSocketChannel = (SocketChannel) selectionKey.channel();
        ConnectionWrapper connectionWrapper = (ConnectionWrapper) selectionKey.attachment();
        connectionWrapper.getConnection().setState(State.READ_REQUSEST);
        //SocketChannel clientSocketChannel = connectionMap.get(connectionSocketChannel);

        boolean isConnected = connectionSocketChannel.finishConnect();
        if (!isConnected) {
            System.out.println("Haven't connected yet");
            //deleteChannels(connectionSocketChannel, clientSocketChannel);
            return;
        }

        System.out.println("Connect response from remote " + connectionSocketChannel.getRemoteAddress());

        //clientSocketChannel.register(selector, SelectionKey.OP_READ);
        SelectionKey key = connectionSocketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        key.attach(connectionWrapper);

    }

    void pauseOption(SelectionKey selectionKey, int option) {
        selectionKey.interestOps(selectionKey.interestOps() & ~option);
    }

    void resumeOption(SelectionKey selectionKey, int option) {
        selectionKey.interestOps(selectionKey.interestOps() | option);
    }

    public Selector getSelector() {
        return selector;
    }

    public Map<SocketChannel, SocketChannel> getConnectionMap() {
        return connectionMap;
    }
}
