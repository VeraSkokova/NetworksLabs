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
                    } else if ("time".equals(fieldName)) {
                        disconnectMessage.setTime(Long.parseLong(jsonParser.getText()));
                    } else if ("hostName".equals(fieldName)) {
                        String hostName = jsonParser.getText();
                        disconnectMessage.setHostName(hostName);
                    } else if ("port".equals(fieldName)) {
                        String port = jsonParser.getText();
                        disconnectMessage.setPort(Integer.parseInt(port));
                    }
                }
            }
        } catch (IOException e) {

        }
        return disconnectMessage;
    }
}
