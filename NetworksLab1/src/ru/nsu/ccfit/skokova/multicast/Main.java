package ru.nsu.ccfit.skokova.multicast;

public class Main {
    public static void main(String[] args) {
        CopyCounter copyCounter = new CopyCounter(args[0]);
        copyCounter.runProgram();
    }
}
