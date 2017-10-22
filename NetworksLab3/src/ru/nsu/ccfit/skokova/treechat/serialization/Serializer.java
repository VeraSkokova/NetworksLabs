package ru.nsu.ccfit.skokova.treechat.serialization;

import ru.nsu.ccfit.skokova.treechat.messages.Message;

import java.io.*;

public class Serializer {
    /*public static byte[] serialize(Message message) throws IOException {
        Gson gson = new Gson();
        String json = gson.toJson(message);
        return json.getBytes();
    }

    public static Message deserialize(String json, GsonBuilder gsonBuilder) throws IOException, ClassNotFoundException {
        Gson gson = gsonBuilder.create();
        return gson.fromJson(json, Message.class);
    }*/

    public static byte[] serialize(Message message) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
                objectOutputStream.writeObject(message);
            }
            return byteArrayOutputStream.toByteArray();
        }
    }

    public static Message deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream b = new ByteArrayInputStream(bytes)) {
            try (ObjectInputStream o = new ObjectInputStream(b)) {
                return (Message) o.readObject();
            }
        }
    }
}
