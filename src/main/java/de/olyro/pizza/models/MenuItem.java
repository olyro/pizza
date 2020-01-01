package de.olyro.pizza.models;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MenuItem {
    public final Item item;
    public final String description;
    public final List<Item> extras;

    public MenuItem(Item item, String description, List<Item> extras) {
        Set<String> itemSizes = item.sizes.stream().map(s -> s.name).collect(Collectors.toSet());
        boolean condition = !extras.stream().allMatch(i -> {
            return i.sizes.stream().map(s -> s.name).collect(Collectors.toSet()).equals(itemSizes);
        });
        if (condition) {
            throw new IllegalArgumentException("Item sizes and extra sizes need to be identical");
        }

        this.item = item;
        this.description = description;
        this.extras = extras;
    }
}
