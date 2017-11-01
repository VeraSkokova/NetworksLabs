package ru.nsu.ccfit.skokova.SimpleTCP.server;

import ru.nsu.ccfit.skokova.SimpleTCP.client.SimpleTcpClientSocket;

import java.net.DatagramSocket;

public class SimpleTcpServerSocket {
    private DatagramSocket datagramServerSocket;

    public SimpleTcpClientSocket accept() {
        return new SimpleTcpClientSocket();
    }
}
