package ru.nsu.ccfit.skokova.SimpleTcp.message.serialization;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import ru.nsu.ccfit.skokova.SimpleTcp.message.Message;
import ru.nsu.ccfit.skokova.SimpleTcp.message.factory.MessageFactory;

import java.io.IOException;

public class MessageDeserializer extends JsonDeserializer<Message> {
    @Override
    public Message deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        while (!jsonParser.isClosed()) {
            JsonToken jsonToken = jsonParser.nextToken();

            if (JsonToken.FIELD_NAME.equals(jsonToken)) {
                String fieldName = jsonParser.getCurrentName();

                jsonToken = jsonParser.nextToken();

                if ("messageType".equals(fieldName)) {
                    String type = jsonParser.getText();
                    try {
                        return MessageFactory.createMessage(type, jsonParser);
                    } catch (IllegalAccessException | InstantiationException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        }
        return null;
    }
}
