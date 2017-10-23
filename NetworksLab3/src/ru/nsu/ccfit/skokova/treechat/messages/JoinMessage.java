package ru.nsu.ccfit.skokova.treechat.messages;

import ru.nsu.ccfit.skokova.treechat.node.TreeNode;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;

public class JoinMessage extends Message {
    public JoinMessage() {
        this.uuid = UUID.randomUUID();
    }

    public JoinMessage(InetSocketAddress inetSocketAddress) {
        super(inetSocketAddress);
        this.uuid = UUID.randomUUID();
    }

    @Override
    public void process(TreeNode treeNode) throws IOException, InterruptedException {
        super.process(treeNode);
        System.out.println("Received JoinMessage from " + senderInetSocketAddress);
        if (!treeNode.getNeighbourAddresses().contains(senderInetSocketAddress)) {
            treeNode.getNeighbourAddresses().add(senderInetSocketAddress);
            System.out.println("Added new child to " + treeNode.getName());
        }
        AckMessage ackMessage = new AckMessage(uuid, treeNode.getMyInetSocketAddress());
        try {
            treeNode.sendDirectMessage(ackMessage, senderInetSocketAddress);
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        } catch (IOException e) {
            System.out.println(e.getMessage());
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
