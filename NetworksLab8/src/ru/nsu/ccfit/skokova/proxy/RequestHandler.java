package ru.nsu.ccfit.skokova.proxy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class RequestHandler {
    private static final int BUF_SIZE = 48 * 1024;//= 20;
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
                        processBody(connectionWrapper.getConnection(), socketChannel, selectionKey);
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

    private void processHeaders(Connection connection, SocketChannel socketChannel, SelectionKey selectionKey) throws IOException {
        System.out.println("Got headers from client");
        ByteBuffer buffer = ByteBuffer.allocate(BUF_SIZE);
        int read = socketChannel.read(buffer);

        if (read != -1) {
            ByteBuffer headerBuffer = ByteBuffer.allocate(read);
            headerBuffer.put(Arrays.copyOf(buffer.array(), read));
            try {
                connection.addHeaders(headerBuffer);
            } catch (InvalidMethodException | InvalidProtocolException e) {
                System.out.println("Invalid protocol/method");
                String errorString = "HTTP/1.0 " + Integer.toString(ErrorCodes.NOT_IMPLEMENTED) + " Not Implemented";
                ByteBuffer byteBuffer = ByteBuffer.wrap(errorString.getBytes(StandardCharsets.ISO_8859_1));
                proxy.resumeOption(selectionKey, SelectionKey.OP_WRITE);
                proxy.pauseOption(selectionKey, SelectionKey.OP_READ);
                connection.addResponse(byteBuffer);
                connection.setState(State.WAIT_RESPONSE);
                connection.setCanBeClosed(true);
                return;
            }
            if (connection.getConnectionInfo() != null) {
                if (connection.isNeedBody()) {
                    connection.setState(State.READ_BODY);
                } else {
                    proxy.pauseOption(selectionKey, SelectionKey.OP_READ);
                    proxy.resumeOption(selectionKey, SelectionKey.OP_WRITE);
                    connection.setState(State.WAIT_RESPONSE);
                    try {
                        proxy.connectChannel(socketChannel, new InetSocketAddress((InetAddress.getByName(connection.getConnectionInfo().getHost())),
                                connection.getConnectionInfo().getPort()), connection);
                    } catch (UnknownHostException e) {
                        System.out.println("Invalid address");
                        String errorString = "HTTP/1.0 " + Integer.toString(ErrorCodes.BAD_GATEWAY) + " Bad Gateway";
                        ByteBuffer byteBuffer = ByteBuffer.wrap(errorString.getBytes(StandardCharsets.ISO_8859_1));
                        connection.setState(State.WAIT_RESPONSE);
                        proxy.pauseOption(selectionKey, SelectionKey.OP_READ);
                        proxy.resumeOption(selectionKey, SelectionKey.OP_WRITE);
                        connection.addResponse(byteBuffer);
                        connection.setCanBeClosed(true);
                    }
                }
            }
        } else {
            System.out.println("Reached end of stream");
            socketChannel.close();
            if (selectionKey.isValid()) {
                selectionKey.cancel();
            }
        }
    }

    private void processBody(Connection connection, SocketChannel socketChannel, SelectionKey selectionKey) throws IOException {
        System.out.println("Got body from client");
        ByteBuffer buffer = ByteBuffer.allocate(BUF_SIZE);
        int read = socketChannel.read(buffer);
        System.out.println("Read in body " + read);

        if (read != -1) {
            ByteBuffer bodyBuffer = ByteBuffer.allocate(read);
            bodyBuffer.put(Arrays.copyOf(buffer.array(), read));
            connection.addBody(bodyBuffer);
            System.out.println("Remaining body : " + connection.getRemainBodyLength());

            if (connection.getRemainBodyLength() == 0) {
                connection.setState(State.WAIT_RESPONSE);
                proxy.pauseOption(selectionKey, SelectionKey.OP_READ);
                proxy.resumeOption(selectionKey, SelectionKey.OP_WRITE);
                proxy.connectChannel(socketChannel, new InetSocketAddress((InetAddress.getByName(connection.getConnectionInfo().getHost())),
                        connection.getConnectionInfo().getPort()), connection);
            }
        } else {
            System.out.println("Reached end of stream");
            socketChannel.close();
            if (selectionKey.isValid()) {
                selectionKey.cancel();
            }
        }
    }

    private void sendNewRequest(Connection connection, SocketChannel socketChannel) throws IOException {
        if (connection.getRequestBuffer().position() != 0) {
            connection.getRequestBuffer().flip();
        }
        int write = socketChannel.write(connection.getRequestBuffer());
        if (write != 0) {
            System.out.println("Sending new request");
            System.out.println("Write " + write + " bytes");
            connection.setState(State.WRITE_RESPONSE);
        }
        connection.getRequestBuffer().compact();
        if (connection.getRequestBuffer().remaining() == 0) {
            proxy.pauseOption(socketChannel.keyFor(proxy.getSelector()), SelectionKey.OP_WRITE);
            socketChannel.shutdownOutput();
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
            System.out.println("Closing");
            socketChannel.close();
            selectionKey.cancel();
            connection.setState(State.WAIT_RESPONSE);
            connection.setCanBeClosed(true);
            return;
        }
        System.out.println("Read " + read + " bytes");
        ByteBuffer answerBuffer = ByteBuffer.wrap(buffer.array(), 0, read);
        connection.addResponse(answerBuffer);

        SocketChannel anotherSocketChannel = proxy.getConnectionMap().get(socketChannel);
        SelectionKey key = anotherSocketChannel.keyFor(proxy.getSelector());
        proxy.resumeOption(key, SelectionKey.OP_WRITE);
        proxy.pauseOption(key, SelectionKey.OP_READ);
    }

    private void getResponse(Connection connection, SocketChannel socketChannel, SelectionKey selectionKey) throws IOException {
        if (connection.getResponseBuffer() != null) {
            if (connection.getResponseBuffer().position() != 0) {
                connection.getResponseBuffer().flip();
            }
            int write = socketChannel.write(connection.getResponseBuffer());
            if (write != 0) {
                System.out.println("Write " + write + " bytes");
            }
            connection.getResponseBuffer().compact();
            if (connection.isCanBeClosed()) {
                System.out.println("Closing");
                socketChannel.close();
                selectionKey.cancel();
            }
        }
    }
}
