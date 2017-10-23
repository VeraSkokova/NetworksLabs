package ru.nsu.ccfit.skokova.treechat.messages;

import ru.nsu.ccfit.skokova.treechat.node.TreeNode;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;

public class UnjoinMessage extends Message {
    public UnjoinMessage(InetSocketAddress inetSocketAddress) {
        super(inetSocketAddress);
        this.uuid = UUID.randomUUID();
    }

    @Override
    public void process(TreeNode treeNode) {
        //System.out.println("Got UnjoinMessage");
        if (treeNode.getNeighbourAddresses().contains(senderInetSocketAddress)) {
            treeNode.getNeighbourAddresses().remove(senderInetSocketAddress);
        }
        try {
            treeNode.sendDirectMessage(new AckMessage(uuid, senderInetSocketAddress), senderInetSocketAddress);
        } catch (IOException e) {
            System.out.println(e.getMessage());
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
