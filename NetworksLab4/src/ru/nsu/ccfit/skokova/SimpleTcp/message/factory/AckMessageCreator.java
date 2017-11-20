package ru.nsu.ccfit.skokova.SimpleTcp.message.factory;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import ru.nsu.ccfit.skokova.SimpleTcp.message.AckMessage;

import java.io.IOException;

public class AckMessageCreator extends MessageCreator {
    @Override
    AckMessage createMessage(JsonParser jsonParser) {
        AckMessage ackMessage = new AckMessage(-1);
        try {
            while (!jsonParser.isClosed()) {
                JsonToken jsonToken = jsonParser.nextToken();

                if (JsonToken.FIELD_NAME.equals(jsonToken)) {
                    String fieldName = jsonParser.getCurrentName();

                    jsonToken = jsonParser.nextToken();

                    if ("ackId".equals(fieldName)) {
                        Long ackId = Long.parseLong(jsonParser.getText());
                        ackMessage.setAckId(ackId);
                    } else if ("time".equals(fieldName)) {
                        ackMessage.setTime(Long.parseLong(jsonParser.getText()));
                    } else if ("hostName".equals(fieldName)) {
                        String hostName = jsonParser.getText();
                        ackMessage.setHostName(hostName);
                    } else if ("port".equals(fieldName)) {
                        String port = jsonParser.getText();
                        ackMessage.setPort(Integer.parseInt(port));
                    }
                }
            }
        } catch (IOException e) {

        }
        return ackMessage;
    }
}
