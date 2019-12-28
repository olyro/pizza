package de.olyro.pizza.models;

import java.util.List;

public class Item {
    public final String id;
    public final String name;
    public final List<Size> sizes;

    public Item(String id, String name, List<Size> sizes) {
        this.id = id;
        this.name = name;
        this.sizes = sizes;
    }
}