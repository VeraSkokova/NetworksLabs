package ru.nsu.ccfit.skokova.proxy;

public class ConnectionWrapper {
    private Connection connection;
    private boolean isFrom;

    public ConnectionWrapper(Connection connection, boolean isFrom) {
        this.connection = connection;
        this.isFrom = isFrom;
    }

    public Connection getConnection() {
        return connection;
    }

    public boolean isFrom() {
        return isFrom;
    }
}
