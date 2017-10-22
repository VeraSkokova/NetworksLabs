package ru.nsu.ccfit.skokova.treechat.messages;

import ru.nsu.ccfit.skokova.treechat.node.TreeNode;

import java.net.InetSocketAddress;
import java.util.UUID;

public class AckMessage extends Message {
    public AckMessage() {
        this.isA = AckMessage.class.getCanonicalName();
    }

    public AckMessage(UUID uuid, InetSocketAddress inetSocketAddress) {
        this();
        this.uuid = uuid;
        this.senderInetSocketAddress = inetSocketAddress;
    }

    @Override
    public void process(TreeNode treeNode) {
        System.out.println("Received AckMessage");
        if (treeNode.getSentMessages().contains(uuid)) {
            treeNode.getSentMessages().remove(uuid);
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
