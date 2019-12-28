package de.olyro.pizza.models;

import java.util.List;

public class OrderMessage extends Message {
    public final List<Order> orders;

    public OrderMessage(List<Order> orders) {
        super(OrderMessage.class.getSimpleName());
        this.orders = orders;
    }
}