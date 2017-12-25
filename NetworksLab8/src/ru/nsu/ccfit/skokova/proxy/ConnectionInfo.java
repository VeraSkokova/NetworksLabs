package ru.nsu.ccfit.skokova.proxy;

import java.util.Map;

public class ConnectionInfo {
    private String method;
    private String protocol;
    private String host;
    private int port;
    private String pathAndQuery;
    private String version;
    private Map<String, String> headersMap;
    private int contentLength;

    public ConnectionInfo(String method, String protocol, String host, int port, String pathAndQuery, String version, Map<String, String> headersMap) {
        this.method = method;
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.pathAndQuery = pathAndQuery;
        this.version = version;
        this.headersMap = headersMap;

        if (headersMap.containsKey("Content-Length")) {
            contentLength = Integer.parseInt(headersMap.get("Content-Length"));
        } else if (headersMap.containsKey("Content-length")) {
            contentLength = Integer.parseInt(headersMap.get("Content-length"));
        }
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMethod() {
        return method;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public String getPathAndQuery() {
        return pathAndQuery;
    }

    public Map<String, String> getHeadersMap() {
        return headersMap;
    }

    public int getContentLength() {
        return contentLength;
    }
}
