package ru.nsu.ccfit.skokova.treechat.messages;

import ru.nsu.ccfit.skokova.treechat.node.TreeNode;

import java.io.Serializable;
import java.util.UUID;

public interface Message extends Serializable {
    /*byte[] serialize() throws IOException;

    Message deserialize(byte[] bytes) throws IOException, ClassNotFoundException;*/

    void process(TreeNode treeNode);

    UUID getUUID();

    void setUUID(UUID uuid);
}
