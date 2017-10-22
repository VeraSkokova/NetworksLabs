package ru.nsu.ccfit.skokova.treechat.messages;

import ru.nsu.ccfit.skokova.treechat.node.TreeNode;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;

public class NewParentMessage extends Message {
    private InetSocketAddress newParentInetSocketAddress;

    public NewParentMessage() {
    }

    public NewParentMessage(InetSocketAddress inetSocketAddress, InetSocketAddress newParentInetSocketAddress) {
        super(inetSocketAddress);
        this.newParentInetSocketAddress = newParentInetSocketAddress;
    }

    @Override
    public void process(TreeNode treeNode) {
        if (newParentInetSocketAddress.equals(treeNode.getMyInetSocketAddress())) {
            treeNode.setRoot(true);
            treeNode.setParentInetSocketAddress(null);
        } else {
            treeNode.setParentInetSocketAddress(newParentInetSocketAddress);
            JoinMessage joinMessage = new JoinMessage(treeNode.getMyInetSocketAddress());
            try {
                treeNode.sendDirectMessage(joinMessage, newParentInetSocketAddress);
                treeNode.sendDirectMessage(new AckMessage(uuid, senderInetSocketAddress), senderInetSocketAddress);
            } catch (InterruptedException e) {
                System.out.println("Interrupted");
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
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
