package ru.nsu.ccfit.skokova.SimpleTcp.message.idGenerator;

import java.util.concurrent.atomic.AtomicLong;

public class IdGenerator {
    private static final AtomicLong ATOMIC_LONG = new AtomicLong(0);

    public long newId() {
        return ATOMIC_LONG.getAndIncrement();
    }
}
