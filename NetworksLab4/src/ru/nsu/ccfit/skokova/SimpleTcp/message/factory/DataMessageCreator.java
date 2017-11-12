package ru.nsu.ccfit.skokova.SimpleTcp.message.factory;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import ru.nsu.ccfit.skokova.SimpleTcp.message.DataMessage;

import java.io.IOException;

public class DataMessageCreator extends MessageCreator {
    @Override
    DataMessage createMessage(JsonParser jsonParser) {
        DataMessage dataMessage = new DataMessage();
        try {
            while (!jsonParser.isClosed()) {
                JsonToken jsonToken = jsonParser.nextToken();

                if (JsonToken.FIELD_NAME.equals(jsonToken)) {
                    String fieldName = jsonParser.getCurrentName();

                    jsonToken = jsonParser.nextToken();

                    if ("data".equals(fieldName)) {
                        byte[] data = jsonParser.readValueAs(byte[].class);
                        dataMessage.setData(data);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return dataMessage;
    }
}
