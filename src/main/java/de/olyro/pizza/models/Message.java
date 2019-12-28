package de.olyro.pizza.models;

public abstract class Message {
    public final String kind;

    public Message(String kind) {
        this.kind = kind;
    }
}