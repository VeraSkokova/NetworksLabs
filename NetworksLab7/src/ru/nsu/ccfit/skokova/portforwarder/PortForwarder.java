package ru.nsu.ccfit.skokova.portforwarder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

public class PortForwarder {
    private int lPort;
    private int rPort;
    private String rHost;

    private ServerSocketChannel serverSocketChannel;
    private Selector selector;

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
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(lPort));
            serverSocketChannel.configureBlocking(false);

            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (true) {
                selector.select();
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
