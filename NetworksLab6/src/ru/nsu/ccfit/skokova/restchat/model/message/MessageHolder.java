package ru.nsu.ccfit.skokova.restchat.model.message;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

public class MessageHolder {
    @JsonProperty("id")
    private int id;
    @JsonProperty("message")
    private String message;
    @JsonProperty("author")
    private int author;

    @JsonCreator
    public MessageHolder(@JsonProperty("message") String message, @JsonProperty("author") int author) {
        this.message = message;
        this.author = author;
    }

    public int getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public int getAuthor() {
        return author;
    }

    public void setId(int id) {
        this.id = id;
    }
}
