package ru.nsu.ccfit.skokova.proxy;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Connection {
    private State state;
    private ConnectionInfo connectionInfo;
    private ByteBuffer headerBuffer;
    private ByteBuffer bodyBuffer;
    private ByteBuffer responseBuffer;

    private int remainBodyLength;
    private static final String HEADERS_END = "\r\n\r\n";

    public Connection() {
        this.state = State.READ_HEADER;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public ByteBuffer getHeaderBuffer() {
        return headerBuffer;
    }

    public ByteBuffer getBodyBuffer() {
        return bodyBuffer;
    }

    public ByteBuffer getResponseBuffer() {
        return responseBuffer;
    }

    public void addHeaders(ByteBuffer byteBuffer) {
        if (headerBuffer == null) {
            headerBuffer = byteBuffer;
        } else {
            ByteBuffer newBuffer = ByteBuffer.allocate(headerBuffer.capacity() + byteBuffer.capacity());
            newBuffer.put(headerBuffer);
            newBuffer.put(byteBuffer);
            newBuffer.flip();
            headerBuffer = newBuffer;
        }

        byte[] headerBytes = headerBuffer.array();
        int headersEndIndex = -1;
        byte[] headersEndBytes = HEADERS_END.getBytes(StandardCharsets.UTF_8);
        int coincidenceCount = 0;

        for (int i = 0; i < headerBytes.length - headersEndBytes.length + 1; i++) {
            if (headersEndIndex != -1) {
                break;
            }

            for (int j = 0; j < headersEndBytes.length; j++) {
                if (headerBytes[i + j] == headersEndBytes[j]) {
                    coincidenceCount++;
                    if (coincidenceCount == headersEndBytes.length) {
                        headersEndIndex = i + j;
                        break;
                    }
                } else {
                    coincidenceCount = 0;
                    break;
                }
            }
        }

        if (headersEndIndex != -1) {
            ByteBuffer newByteBuffer = ByteBuffer.allocate(headersEndIndex + 1);
            newByteBuffer.put(Arrays.copyOf(headerBuffer.array(), headersEndIndex + 1));
            newByteBuffer.flip();

            if (headersEndIndex != headerBuffer.array().length - 1) {
                bodyBuffer = ByteBuffer.wrap(headerBuffer.array(), headersEndIndex + 1, headerBuffer.array().length - headersEndIndex - 1);
            }

            headerBuffer = newByteBuffer;
            ConnectionInfo connectionInfo = HeaderParser.parseHeaders(headerBuffer.array());
            if (connectionInfo != null) {
                this.connectionInfo = connectionInfo;
                this.remainBodyLength = connectionInfo.getContentLength();
                if (bodyBuffer != null) {
                    remainBodyLength -= bodyBuffer.array().length;
                }
            }
        }
    }

    public void addBody(ByteBuffer byteBuffer) {
        if (bodyBuffer == null) {
            bodyBuffer = byteBuffer;
        } else {
            ByteBuffer newBuffer = ByteBuffer.allocate(bodyBuffer.capacity() + byteBuffer.capacity());
            newBuffer.put(bodyBuffer);
            newBuffer.put(byteBuffer);
            newBuffer.flip();
            bodyBuffer = newBuffer;

            remainBodyLength -= byteBuffer.array().length;
        }
    }

    public void addResponse(ByteBuffer byteBuffer) {
        if (responseBuffer == null) {
            responseBuffer = byteBuffer;
        } else {
            ByteBuffer newBuffer = ByteBuffer.allocate(responseBuffer.capacity() + byteBuffer.capacity());
            newBuffer.put(responseBuffer);
            newBuffer.put(byteBuffer);
            newBuffer.flip();
            responseBuffer = newBuffer;
        }
    }

    public ConnectionInfo getConnectionInfo() {
        return connectionInfo;
    }

    public int getRemainBodyLength() {
        return remainBodyLength;
    }
}
