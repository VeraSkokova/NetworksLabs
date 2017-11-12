package ru.nsu.ccfit.skokova.SimpleTcp.message.factory;

import org.codehaus.jackson.JsonParser;
import ru.nsu.ccfit.skokova.SimpleTcp.message.Message;

public abstract class MessageCreator {
    abstract Message createMessage(JsonParser jsonParser);
}
