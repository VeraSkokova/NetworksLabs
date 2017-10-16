package ru.nsu.ccfit.skokova.treechat.messages;

import ru.nsu.ccfit.skokova.treechat.node.TreeNode;

import java.util.UUID;

public class AckMessage implements Message {
    private UUID uuid;

    public AckMessage(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public void process(TreeNode treeNode) {
        System.out.println("Received AckMessage");
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
