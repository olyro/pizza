package de.olyro.pizza.models;

import java.util.List;

public class OrderItem {
    public final String id;
    public final String size;
    public final List<String> extraIds;

    public OrderItem(String id, String size, List<String> extraIds) {
        this.id = id;
        this.size = size;
        this.extraIds = extraIds;
    }
}