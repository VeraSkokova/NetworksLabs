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
    private static final int BUF_SIZE = 200;//= 50;//= 48 * 1024;
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
                case READ_HEADER:
                    if (selectionKey.isReadable()) {
                        processHeaders(connectionWrapper.getConnection(), socketChannel, selectionKey);
                    }
                    break;
                case READ_BODY:
                    if (selectionKey.isReadable()) {
                        processBody(connectionWrapper.getConnection(), socketChannel);
                    }
                    break;
                case WAIT_RESPONSE:
                    if (selectionKey.isWritable()) {
                        getResponse(connectionWrapper.getConnection(), socketChannel, selectionKey);
                    }
                    break;
                default:
                    break;
            }
        } else {
            switch (connectionWrapper.getConnection().getState()) {
                case READ_REQUSEST:
                    if (selectionKey.isWritable()) {
                        sendNewRequest(connectionWrapper.getConnection(), socketChannel);
                    }
                    break;
                case WRITE_RESPONSE:
                    if (selectionKey.isReadable()) {
                        readAnswer(connectionWrapper.getConnection(), socketChannel, selectionKey);
                    }
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

    private void processHeaders(Connection connection, SocketChannel socketChannel, SelectionKey selectionKey) throws IOException {
        //System.out.println("Send headers");
        ByteBuffer buffer = ByteBuffer.allocate(BUF_SIZE);
        int read = socketChannel.read(buffer);

        if (read != -1) {
            ByteBuffer headerBuffer = ByteBuffer.allocate(read);
            headerBuffer.put(Arrays.copyOf(buffer.array(), read));
            try {
                connection.addHeaders(headerBuffer);
            } catch (InvalidMethodException | InvalidProtocolException e) {
                System.out.println("Invalid protocol/method");
                String errorString = "HTTP/1.0 " + Integer.toString(ErrorCodes.NOT_IMPLEMENTED) + " Not implemented\r\n";
                ByteBuffer byteBuffer = ByteBuffer.wrap(errorString.getBytes(StandardCharsets.UTF_8));
                socketChannel.write(byteBuffer);
                socketChannel.close();
                selectionKey.cancel();
            }
            if (connection.getConnectionInfo() != null) {
                if (connection.isNeedBody()) {
                    //System.out.println("Need read body");
                    connection.setState(State.READ_BODY);
                } else {
                    connection.setState(State.WAIT_RESPONSE);
                    proxy.connectChannel(socketChannel, new InetSocketAddress((InetAddress.getByName(connection.getConnectionInfo().getHost())),
                            connection.getConnectionInfo().getPort()), connection);
                }
            }
        } else {
            socketChannel.close();
            if (selectionKey.isValid()) {
                selectionKey.cancel();
            }
        }
    }

    private void processBody(Connection connection, SocketChannel socketChannel) throws IOException {
        //System.out.println("Send body");
        ByteBuffer buffer = ByteBuffer.allocate(BUF_SIZE);
        int read = socketChannel.read(buffer);
        System.out.println("Read in body " + read);

        if (read != -1) {
            ByteBuffer bodyBuffer = ByteBuffer.allocate(read);
            bodyBuffer.put(Arrays.copyOf(buffer.array(), read));
            connection.addBody(bodyBuffer);

            if (connection.getRemainBodyLength() <= connection.getConnectionInfo().getContentLength()) {
                connection.setState(State.WAIT_RESPONSE);
                proxy.connectChannel(socketChannel, new InetSocketAddress((InetAddress.getByName(connection.getConnectionInfo().getHost())),
                        connection.getConnectionInfo().getPort()), connection);
            }
        } else {
            connection.setState(State.WAIT_RESPONSE);
        }
    }

    private void sendNewRequest(Connection connection, SocketChannel socketChannel) throws IOException {
        //System.out.println("Send new request");
        byte[] newHeaders = makeNewRequest(connection.getConnectionInfo());
        System.out.println("Writing to channel " + new String(newHeaders, StandardCharsets.UTF_8));
        ByteBuffer buffer = ByteBuffer.wrap(newHeaders);
        int write = 0;
        if (connection.getConnectionInfo().getMethod().equals("POST")) {
            byte[] bodyBytes = connection.getBodyBuffer().array();
            ByteBuffer postBuffer = ByteBuffer.allocate(buffer.capacity() + connection.getBodyBuffer().capacity());
            postBuffer.put(newHeaders);
            postBuffer.put(bodyBytes);
            postBuffer.flip();
            write = socketChannel.write(postBuffer);
        } else {
            write = socketChannel.write(buffer);
        }
        if (write != 0) {
            System.out.println("Send new request");
            System.out.println("Write " + write + " bytes");
        }
        if (write != 0) {
            connection.setState(State.WRITE_RESPONSE);
        }
    }

    private void readAnswer(Connection connection, SocketChannel socketChannel, SelectionKey selectionKey) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(BUF_SIZE);
        int read = socketChannel.read(buffer);
        if (read == 0) {
            return;
        }
        System.out.println("Read answer");
        if (read == -1) {
            //System.out.println("Received response from " + socketChannel.getRemoteAddress());
            socketChannel.close();
            selectionKey.cancel();
            connection.setState(State.WAIT_RESPONSE);
            connection.setCanBeClosed(true);
            return;
        }
        System.out.println("Read " + read + " bytes");
        ByteBuffer answerBuffer = ByteBuffer.wrap(buffer.array(), 0, read);
        connection.addResponse(answerBuffer);
        //System.out.println("Answer is " + new String(buffer.array(), StandardCharsets.ISO_8859_1));

        Set<SelectionKey> selectionKeys = proxy.getSelector().keys();
        SocketChannel anotherSocketChannel = proxy.getConnectionMap().get(socketChannel);
        for (SelectionKey key : selectionKeys) {
            if (key.channel().equals(anotherSocketChannel)) {
                proxy.resumeOption(key, SelectionKey.OP_WRITE);
                proxy.pauseOption(key, SelectionKey.OP_READ);
            }
        }
    }

    private void getResponse(Connection connection, SocketChannel socketChannel, SelectionKey selectionKey) throws IOException {
        if (connection.getResponseBuffer() != null) {
            int write = socketChannel.write(connection.getResponseBuffer());
            if (write != 0) {
                System.out.println("Get response");
                System.out.println("Write " + write + " bytes");
            }
            if (connection.isCanBeClosed()) {
                socketChannel.close();
                selectionKey.cancel();
            }
        }
    }

    private void changeConnectionInfo(ConnectionInfo connectionInfo) {
        if (connectionInfo.getPort() == -1) {
            connectionInfo.setPort(80);
        }
        connectionInfo.getHeadersMap().put("Connection", "close");
    }
}
