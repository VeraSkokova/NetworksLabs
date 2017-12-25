package ru.nsu.ccfit.skokova.proxy;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HeaderParser {
    private static final String headerSplitter = "\r\n";

    public static ConnectionInfo parseHeaders(byte[] headersByte) {
        try {
            String headers = new String(headersByte, StandardCharsets.UTF_8);
            String[] headersRows = headers.split(headerSplitter);
            String[] startString = headersRows[0].split(" ");

            String method = startString[0];

            URL url = new URL(startString[1]);

            String protocol = url.getProtocol();
            String host = url.getHost();
            int port = url.getPort();
            String pathAndQuery = url.getPath();
            if (url.getQuery() != null) {
                pathAndQuery += "?" + url.getQuery();
            }

            String version = startString[2].split("/")[1];

            Map<String, String> headerMap = new HashMap<>();
            for (int i = 1; i < headersRows.length; i++) {
                String[] headerRow = headersRows[i].split(": ");
                headerMap.put(headerRow[0], headerRow[1]);
            }

            if (isValidMethod(method) && isValidProtocol(protocol) && isValidVersion(version)) {
                return new ConnectionInfo(method, protocol, host, port, pathAndQuery, version, headerMap);
            }
        } catch (MalformedURLException e) {
            System.err.println(e.getMessage());
        }

        return null;
    }

    private static boolean isValidMethod(String method) {
        return ((method.equals("GET")) || (method.equals("POST")) || method.equals("HEAD"));
    }

    private static boolean isValidProtocol(String protocol) {
        return protocol.equalsIgnoreCase("HTTP");
    }

    private static boolean isValidVersion(String version) {
        return ((version.equals("1.0")) || (version.equals("1.1")));
    }
}