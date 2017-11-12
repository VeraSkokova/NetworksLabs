package ru.nsu.ccfit.skokova.md5cracker.task;

public class Task {
    private final long length;
    private final long start;
    private final long end;

    public Task(long length, long start, long end) {
        this.length = length;
        this.start = start;
        this.end = end;
    }

    public long getLength() {
        return length;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }
}
