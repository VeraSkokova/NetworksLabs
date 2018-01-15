package ru.nsu.ccfit.skokova.proxy;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

public class Connection {
    private static final String HEADER_SPLITTER = "\r\n";
    private static final String HEADERS_END = "\r\n\r\n";
    private State state;
    private ConnectionInfo connectionInfo;
    private ByteBuffer requestBuffer;
    private ByteBuffer responseBuffer;
    private boolean canBeClosed = false;
    private boolean needBody = false;
    private int remainBodyLength;

    public Connection() {
        this.state = State.READ_HEADER;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public ByteBuffer getResponseBuffer() {
        return responseBuffer;
    }

    public boolean isCanBeClosed() {
        return canBeClosed;
    }

    public void setCanBeClosed(boolean canBeClosed) {
        this.canBeClosed = canBeClosed;
    }

    public void addHeaders(ByteBuffer byteBuffer) throws InvalidMethodException, InvalidProtocolException {
        System.out.println();
        if (requestBuffer == null) {
            requestBuffer = byteBuffer;
        } else {
            byte[] newBytes = new byte[requestBuffer.array().length + byteBuffer.array().length];
            System.arraycopy(requestBuffer.array(), 0, newBytes, 0, requestBuffer.array().length);
            System.arraycopy(byteBuffer.array(), 0, newBytes, requestBuffer.array().length, byteBuffer.array().length);
            ByteBuffer newBuffer = ByteBuffer.allocate(newBytes.length);
            newBuffer.put(newBytes);
            requestBuffer = newBuffer;
        }

        byte[] headerBytes = requestBuffer.array();//= headerBuffer.array();
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
            byte[] headerPart = Arrays.copyOf(requestBuffer.array(), headersEndIndex + 1);
            ConnectionInfo connectionInfo = HeaderParser.parseHeaders(headerPart);
            if (connectionInfo != null) {
                this.connectionInfo = connectionInfo;
                byte[] oldBytes = Arrays.copyOf(requestBuffer.array(), requestBuffer.array().length);
                makeNewRequest(connectionInfo);
                this.remainBodyLength = connectionInfo.getContentLength();
                if (headersEndIndex != oldBytes.length - 1) {
                    byte[] bodyBytes = Arrays.copyOfRange(oldBytes, headersEndIndex + 1, oldBytes.length);
                    ByteBuffer bodyBuffer = ByteBuffer.wrap(bodyBytes);
                    addBody(bodyBuffer);
                    if (remainBodyLength != 0) {
                        needBody = true;
                    }
                }
            }
        }
    }

    public void addBody(ByteBuffer byteBuffer) {
        byte[] newBytes = new byte[requestBuffer.array().length + byteBuffer.array().length];
        System.arraycopy(requestBuffer.array(), 0, newBytes, 0, requestBuffer.array().length);
        System.arraycopy(byteBuffer.array(), 0, newBytes, requestBuffer.array().length, byteBuffer.array().length);
        ByteBuffer newBuffer = ByteBuffer.allocate(newBytes.length);
        newBuffer.put(newBytes);
        requestBuffer = newBuffer;

        remainBodyLength -= byteBuffer.array().length;
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

    public boolean isNeedBody() {
        return needBody;
    }

    public ByteBuffer getRequestBuffer() {
        return requestBuffer;
    }

    private void makeNewRequest(ConnectionInfo connectionInfo) {
        changeConnectionInfo(connectionInfo);
        String result = connectionInfo.getMethod() + " ";
        result += "/" + connectionInfo.getPathAndQuery() + " " + "HTTP/" + connectionInfo.getVersion() + HEADER_SPLITTER;
        Set<String> keySet = connectionInfo.getHeadersMap().keySet();
        for (Iterator<String> iterator = keySet.iterator(); iterator.hasNext(); ) {
            String key = iterator.next();
            String header = key + ": " + connectionInfo.getHeadersMap().get(key) + HEADER_SPLITTER;
            result += header;
        }
        result += HEADER_SPLITTER;

        requestBuffer = ByteBuffer.wrap(result.getBytes(StandardCharsets.UTF_8));
    }

    private void changeConnectionInfo(ConnectionInfo connectionInfo) {
        if (connectionInfo.getPort() == -1) {
            connectionInfo.setPort(80);
        }
        connectionInfo.getHeadersMap().put("Connection", "close");
    }


}
