package ru.nsu.ccfit.skokova.treechat.messages;

import ru.nsu.ccfit.skokova.treechat.node.TreeNode;

import java.io.IOException;
import java.util.UUID;

public class UnjoinMessage extends Message {
    public UnjoinMessage() {
        this.isA = UnjoinMessage.class.getCanonicalName();
    }

    @Override
    public void process(TreeNode treeNode) {
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
