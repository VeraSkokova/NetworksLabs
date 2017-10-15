package ru.nsu.ccfit.skokova.treechat.messages;

import java.util.HashMap;

public class MessageCreator {
    private static final int JOIN_MSG_NUMBER = 0;
    private static final int ACK_MSG_NUMBER = 1;
    private static final int TEXT_MSG_NUMBER = 2;
    private static final int NEW_PARENT_MSG_NUMBER = 3;
    private static final int UNJOIN_MSG_NUMBER = 4;

    private static final HashMap<Integer, Class> creators = new HashMap<>();

    static {
        creators.put(JOIN_MSG_NUMBER, JoinMessage.class);
        creators.put(ACK_MSG_NUMBER, AckMessage.class);
        creators.put(TEXT_MSG_NUMBER, TextMessage.class);
        creators.put(NEW_PARENT_MSG_NUMBER, NewParentMessage.class);
        creators.put(UNJOIN_MSG_NUMBER, UnjoinMessage.class);
    }

    public static Class getClassByIndex(int i) {
        return creators.get(i);
    }
}
