package ru.nsu.ccfit.skokova.treechat.messages;

import ru.nsu.ccfit.skokova.treechat.node.TreeNode;

import java.net.InetSocketAddress;
import java.util.UUID;

public class TextMessage implements Message {
    private UUID uuid;
    private InetSocketAddress author;
    private String text;

    public TextMessage(UUID uuid, InetSocketAddress author, String text) {
        this.uuid = uuid;
        this.author = author;
        this.text = text;
    }

    @Override
    public void process(TreeNode treeNode) {
        System.out.println("Received TextMessage from " + author);
        System.out.println(text);
        try {
            treeNode.getMessagesToSend().put(this);
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
