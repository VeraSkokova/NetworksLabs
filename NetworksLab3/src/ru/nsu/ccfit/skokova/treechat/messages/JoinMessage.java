package ru.nsu.ccfit.skokova.treechat.messages;

import ru.nsu.ccfit.skokova.treechat.node.TreeNode;

import java.net.InetSocketAddress;
import java.util.UUID;

public class JoinMessage implements Message {
    private InetSocketAddress author;
    private UUID uuid;

    @Override
    public void process(TreeNode treeNode) {
        System.out.println("Received JoinMessage from " + author);
        if (!treeNode.getNeighbourAddresses().contains(author)) {
            treeNode.getNeighbourAddresses().add(author);
        }
        System.out.println("Added new child to " + treeNode.getName());
        AckMessage ackMessage = new AckMessage(uuid);
        try {
            treeNode.getMessagesToSend().put(ackMessage);
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        }
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }
}
