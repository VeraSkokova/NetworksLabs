package ru.nsu.ccfit.skokova.treechat;

import ru.nsu.ccfit.skokova.treechat.node.TreeNode;

public class Main {
    private static final int SIMPLE_ARGS_SIZE = 3;
    private static final int ROOT_ARGS_SIZE = 5;

    public static void main(String[] args) {
        TreeNode treeNode = null;
        if (args.length >= SIMPLE_ARGS_SIZE) {
            String nodeName = args[0];
            int percentageLoss = Integer.parseInt(args[1]);
            int port = Integer.parseInt(args[2]);
            if (args.length == ROOT_ARGS_SIZE) {
                String parentAddress = args[3];
                int parentPort = Integer.parseInt(args[4]);
                treeNode = new TreeNode(nodeName, percentageLoss, port, parentAddress, parentPort);
            } else if (args.length == SIMPLE_ARGS_SIZE) {
                treeNode = new TreeNode(nodeName, percentageLoss, port);
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
