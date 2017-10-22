package ru.nsu.ccfit.skokova.treechat.messages;

import ru.nsu.ccfit.skokova.treechat.node.TreeNode;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.UUID;

public abstract class Message implements Serializable {
    protected UUID uuid;
    protected String isA;

    protected InetSocketAddress senderInetSocketAddress;

    public Message() {
    }

    public Message(InetSocketAddress inetSocketAddress) {
        this.senderInetSocketAddress = inetSocketAddress;
    }

    public abstract void process(TreeNode treeNode);

    public UUID getUUID() {
        return uuid;
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public InetSocketAddress getSenderInetSocketAddress() {
        return senderInetSocketAddress;
    }
}
