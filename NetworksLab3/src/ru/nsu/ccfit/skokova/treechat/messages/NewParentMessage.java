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
        this.uuid = UUID.randomUUID();
        this.newParentInetSocketAddress = newParentInetSocketAddress;
    }

    @Override
    public void process(TreeNode treeNode) throws IOException, InterruptedException {
        //System.out.println("Got NewParent message");
        super.process(treeNode);
        treeNode.getNeighbourAddresses().remove(treeNode.getParentInetSocketAddress());
        if (newParentInetSocketAddress.equals(treeNode.getMyInetSocketAddress())) {
            //System.out.println("I'm new root");
            treeNode.setRoot(true);
            treeNode.setParentInetSocketAddress(null);
            treeNode.sendDirectMessage(new AckMessage(uuid, senderInetSocketAddress), senderInetSocketAddress);
        } else {
            //System.out.println("My new parent is " + newParentInetSocketAddress);
            treeNode.setParentInetSocketAddress(newParentInetSocketAddress);
            JoinMessage joinMessage = new JoinMessage(treeNode.getMyInetSocketAddress());
            try {
                //System.out.println("Send new JoinMessage");
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
