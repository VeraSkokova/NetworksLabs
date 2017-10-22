package ru.nsu.ccfit.skokova.treechat.messages;

import ru.nsu.ccfit.skokova.treechat.node.TreeNode;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;

public class TextMessage extends Message {
    private String author;
    private String text;

    public TextMessage() {
        this.isA = TextMessage.class.getCanonicalName();
    }

    public TextMessage(UUID uuid, String author, String text, InetSocketAddress inetSocketAddress) {
        this();
        this.uuid = uuid;
        this.author = author;
        this.text = text;
        this.senderInetSocketAddress = inetSocketAddress;
    }

    @Override
    public void process(TreeNode treeNode) {
        System.out.println("Received TextMessage from " + author);
        System.out.println(author + " : " + text);
        InetSocketAddress previousAuthorAddress = senderInetSocketAddress;
        senderInetSocketAddress = treeNode.getMyInetSocketAddress();
        try {
            treeNode.sendMessage(this, previousAuthorAddress);
            treeNode.sendDirectMessage(new AckMessage(uuid, treeNode.getMyInetSocketAddress()), previousAuthorAddress);
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
