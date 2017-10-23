package ru.nsu.ccfit.skokova.treechat.messages;

import ru.nsu.ccfit.skokova.treechat.node.TreeNode;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.UUID;

public abstract class Message implements Serializable {
    protected UUID uuid;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message = (Message) o;

        return message.uuid.equals(uuid);
    }

    @Override
    public int hashCode() {
        int result = uuid != null ? uuid.hashCode() : 0;
        result = 31 * result + (senderInetSocketAddress != null ? senderInetSocketAddress.hashCode() : 0);
        return result;
    }
}
