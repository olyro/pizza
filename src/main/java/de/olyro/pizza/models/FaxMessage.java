package de.olyro.pizza.models;

public class FaxMessage extends Message {
    public final FaxStatus status;

    public FaxMessage(FaxStatus status) {
        super(FaxMessage.class.getSimpleName());
        this.status = status;
    }
}