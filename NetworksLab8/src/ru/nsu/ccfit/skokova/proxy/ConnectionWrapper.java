package ru.nsu.ccfit.skokova.proxy;

import java.nio.channels.SelectionKey;

public class ConnectionWrapper {
    private Connection connection;
    private boolean isFrom;
    private SelectionKey anotherSelectionKey;

    public ConnectionWrapper(Connection connection, boolean isFrom, SelectionKey anotherSelectionKey) {
        this.connection = connection;
        this.isFrom = isFrom;
        this.anotherSelectionKey = anotherSelectionKey;
    }

    public Connection getConnection() {
        return connection;
    }

    public boolean isFrom() {
        return isFrom;
    }
}
