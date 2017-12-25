package ru.nsu.ccfit.skokova.proxy;

public enum State {
    READ_HEADER,
    READ_BODY,
    WAIT_RESPONSE,
    READ_REQUSEST,
    WRITE_RESPONSE
}
