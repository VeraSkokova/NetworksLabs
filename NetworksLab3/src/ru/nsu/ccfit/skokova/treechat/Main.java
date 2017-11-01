package ru.nsu.ccfit.skokova.treechat;

import ru.nsu.ccfit.skokova.treechat.node.TreeNode;

public class Main {
    private static final int SIMPLE_ARGS_SIZE = 4;
    private static final int ROOT_ARGS_SIZE = 6;

    public static void main(String[] args) {
        TreeNode treeNode = null;
        if (args.length >= SIMPLE_ARGS_SIZE) {
            String nodeName = args[0];
            int percentageLoss = Integer.parseInt(args[1]);
            String address = args[2];
            int port = Integer.parseInt(args[3]);
            if (args.length == ROOT_ARGS_SIZE) {
                String parentAddress = args[4];
                int parentPort = Integer.parseInt(args[5]);
                treeNode = new TreeNode(nodeName, percentageLoss, port, address, parentAddress, parentPort);
            } else if (args.length == SIMPLE_ARGS_SIZE) {
                treeNode = new TreeNode(nodeName, percentageLoss, address, port);
            } else {
                System.out.println("Invalid arguments size");
                System.exit(1);
            }
        } else {
            System.out.println("Invalid arguments size");
            System.exit(1);
        }

        treeNode.start();
    }
}
