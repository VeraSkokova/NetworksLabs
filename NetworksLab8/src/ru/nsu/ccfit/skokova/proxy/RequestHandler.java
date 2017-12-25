package ru.nsu.ccfit.skokova.proxy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

public class RequestHandler {
    private static final int BUF_SIZE = 48 * 1024;
    private static final String headerSplitter = "\r\n";
    private Proxy proxy;

    public RequestHandler(Proxy proxy) {
        this.proxy = proxy;
    }

    public void handleRequest(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ConnectionWrapper connectionWrapper = (ConnectionWrapper) selectionKey.attachment();

        if (connectionWrapper.isFrom()) {
            switch (connectionWrapper.getConnection().getState()) {
                case READ_HEADER: //key.isReadable()
                    processHeaders(connectionWrapper.getConnection(), socketChannel);
                    break;
                case READ_BODY: //key.isReadable()
                    processBody(connectionWrapper.getConnection(), socketChannel);
                    break;
                case WAIT_RESPONSE: //key.isWritable()
                    getResponse(connectionWrapper.getConnection(), socketChannel);
                    break;
                default:
                    break;
            }
        } else {
            switch (connectionWrapper.getConnection().getState()) {
                case READ_REQUSEST: //key.isWritable()
                    sendNewRequest(connectionWrapper.getConnection(), socketChannel);
                    break;
                case WRITE_RESPONSE: //key.isReadable()
                    readAnswer(connectionWrapper.getConnection(), socketChannel, selectionKey);
                    break;
                default:
                    break;
            }
        }
    }

    public byte[] makeNewRequest(ConnectionInfo connectionInfo) {
        changeConnectionInfo(connectionInfo);
        String result = connectionInfo.getMethod() + " ";
        result += "/" + connectionInfo.getPathAndQuery() + " " + "HTTP/" + connectionInfo.getVersion() + headerSplitter;
        Set<String> keySet = connectionInfo.getHeadersMap().keySet();
        for (Iterator<String> iterator = keySet.iterator(); iterator.hasNext(); ) {
            String key = iterator.next();
            String header = key + ": " + connectionInfo.getHeadersMap().get(key) + headerSplitter;
            result += header;
        }
        result += headerSplitter;

        return result.getBytes(StandardCharsets.UTF_8);
    }

    private void processHeaders(Connection connection, SocketChannel socketChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(BUF_SIZE);
        int read = socketChannel.read(buffer);

        if (read != -1) {
            ByteBuffer headerBuffer = ByteBuffer.allocate(read);
            headerBuffer.put(Arrays.copyOf(buffer.array(), read));
            connection.addHeaders(headerBuffer);
            if (connection.getConnectionInfo() != null) {
                if (connection.getConnectionInfo().getMethod().equals("POST")) {
                    connection.setState(State.READ_BODY);
                } else {
                    connection.setState(State.WAIT_RESPONSE);
                    proxy.connectChannel(new InetSocketAddress((InetAddress.getByName(connection.getConnectionInfo().getHost())),
                            connection.getConnectionInfo().getPort()), connection);
                }
            }
        }
    }

    private void processBody(Connection connection, SocketChannel socketChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(BUF_SIZE);
        int read = socketChannel.read(buffer);

        if (read != -1) {
            ByteBuffer bodyBuffer = ByteBuffer.allocate(read);
            bodyBuffer.put(Arrays.copyOf(buffer.array(), read));
            connection.addBody(bodyBuffer);

            if (connection.getRemainBodyLength() <= connection.getConnectionInfo().getContentLength()) {
                connection.setState(State.WAIT_RESPONSE);
                proxy.connectChannel(new InetSocketAddress((InetAddress.getByName(connection.getConnectionInfo().getHost())),
                        connection.getConnectionInfo().getPort()), connection);
            }
        }
    }

    private void sendNewRequest(Connection connection, SocketChannel socketChannel) throws IOException {
        byte[] newHeaders = makeNewRequest(connection.getConnectionInfo());
        ByteBuffer buffer;
        if (connection.getConnectionInfo().getMethod().equals("POST")) {
            byte[] bodyBytes = connection.getBodyBuffer().array();
            buffer = ByteBuffer.allocate(newHeaders.length + bodyBytes.length);
            buffer.put(newHeaders);
            buffer.put(bodyBytes);
        } else {
            buffer = ByteBuffer.allocate(newHeaders.length);
            buffer.put(newHeaders);
        }
        int write = socketChannel.write(buffer);
        System.out.println("Write " + write + " bytes");
    }

    private void readAnswer(Connection connection, SocketChannel socketChannel, SelectionKey selectionKey) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(BUF_SIZE);
        int read = socketChannel.read(buffer);
        if (read == -1) {
            System.out.println("Received response from " + socketChannel.getRemoteAddress());
            socketChannel.close();
            selectionKey.cancel();
            connection.setState(State.WAIT_RESPONSE);
            return;
        }
        ByteBuffer answerBuffer = ByteBuffer.allocate(read);
        answerBuffer.put(Arrays.copyOf(buffer.array(), read));
        connection.addResponse(answerBuffer);
    }

    private void getResponse(Connection connection, SocketChannel socketChannel) throws IOException {
        if (connection.getResponseBuffer() != null) {
            socketChannel.write(connection.getResponseBuffer());

        }
    }

    private void changeConnectionInfo(ConnectionInfo connectionInfo) {
        if (connectionInfo.getPort() == -1) {
            connectionInfo.setPort(80);
        }
        connectionInfo.getHeadersMap().put("Connection", "close");
    }
}
