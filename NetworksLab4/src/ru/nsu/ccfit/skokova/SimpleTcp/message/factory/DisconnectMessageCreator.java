package ru.nsu.ccfit.skokova.SimpleTcp.message.factory;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import ru.nsu.ccfit.skokova.SimpleTcp.message.DisconnectMessage;
import ru.nsu.ccfit.skokova.SimpleTcp.message.Message;

import java.io.IOException;

public class DisconnectMessageCreator extends MessageCreator {
    @Override
    DisconnectMessage createMessage(JsonParser jsonParser) {
        DisconnectMessage disconnectMessage = new DisconnectMessage();
        try {
            while (!jsonParser.isClosed()) {
                JsonToken jsonToken = jsonParser.nextToken();

                if (JsonToken.FIELD_NAME.equals(jsonToken)) {
                    String fieldName = jsonParser.getCurrentName();

                    jsonToken = jsonParser.nextToken();

                    if ("id".equals(fieldName)) {
                        Long id = Long.parseLong(jsonParser.getText());
                        disconnectMessage.setId(id);
                    }
                }
            }
        } catch (IOException e) {

        }
        return disconnectMessage;
    }
}
