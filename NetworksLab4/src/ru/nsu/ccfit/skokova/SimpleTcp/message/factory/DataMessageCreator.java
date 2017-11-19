package ru.nsu.ccfit.skokova.SimpleTcp.message.factory;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import ru.nsu.ccfit.skokova.SimpleTcp.message.DataMessage;

import java.io.IOException;
import java.util.UUID;

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
                    } else if ("hostName".equals(fieldName)) {
                        String hostName = jsonParser.getText();
                        dataMessage.setHostName(hostName);
                    } else if ("port".equals(fieldName)) {
                        String port = jsonParser.getText();
                        dataMessage.setPort(Integer.parseInt(port));
                    } else if ("nextId".equals(fieldName)) {
                        String idString = jsonParser.getText();
                        if (!"null".equals(idString)) {
                            long uuid = Long.valueOf(idString);
                            dataMessage.setNextId(uuid);
                        }
                    } else if ("id".equals(fieldName)) {
                        String idString = jsonParser.getText();
                        if (!"null".equals(idString)) {
                            long uuid = Long.valueOf(idString);
                            dataMessage.setId(uuid);
                        }
                    }
                }
            }
        } catch (IOException e) {
            //System.out.println(e.getMessage());
        }
        return dataMessage;
    }
}
