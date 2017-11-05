package ru.nsu.ccfit.skokova.SimpleTcp.message.factory;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import ru.nsu.ccfit.skokova.SimpleTcp.message.ConnectMessage;

import java.io.IOException;

public class ConnectMessageCreator extends MessageCreator {
    @Override
    ConnectMessage createMessage(JsonParser jsonParser) {
        ConnectMessage connectMessage = new ConnectMessage();
        try {
            while (!jsonParser.isClosed()) {
                JsonToken jsonToken = jsonParser.nextToken();

                if (JsonToken.FIELD_NAME.equals(jsonToken)) {
                    String fieldName = jsonParser.getCurrentName();

                    jsonToken = jsonParser.nextToken();

                    if ("hostName".equals(fieldName)) {
                        String hostName = jsonParser.getText();
                        connectMessage.setHostName(hostName);
                    } else if ("port".equals(fieldName)) {
                        String port = jsonParser.getText();
                        connectMessage.setPort(Integer.parseInt(port));
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return connectMessage;
    }
}
