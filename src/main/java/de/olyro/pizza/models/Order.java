package de.olyro.pizza.models;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.olyro.pizza.Main;

public class Order {
    public final String id;
    public final String name;
    public final boolean payed;
    public final List<OrderItem> items;

    public Order(String id, String name, boolean payed, List<OrderItem> items) {
        this.id = id;
        this.name = name;
        this.payed = payed;
        this.items = items;
    }

    public static List<Order> getOrders() throws SQLException {
        Gson gson = new GsonBuilder().create();
        try (Connection connection = Main.getConnection()) {
            PreparedStatement s = connection
                    .prepareStatement("SELECT * FROM orders WHERE hidden = false ORDER BY date");
            ResultSet rs = s.executeQuery();
            var result = new ArrayList<Order>();
            while (rs.next()) {
                result.add(gson.fromJson(rs.getString("data"), Order.class));
            }

            return result;
        }
    }

    public static Optional<Order> getOrder(String id) throws SQLException {
        Gson gson = new GsonBuilder().create();
        try (Connection connection = Main.getConnection()) {
            PreparedStatement s = connection.prepareStatement("SELECT * FROM orders WHERE id = ?");
            s.setString(1, id);
            ResultSet rs = s.executeQuery();
            return rs.next() ? Optional.of(gson.fromJson(rs.getString("data"), Order.class)) : Optional.empty();
        }
    }

    public static boolean deleteOrder(String id) throws SQLException {
        try (Connection connection = Main.getConnection()) {
            PreparedStatement s = connection.prepareStatement("DELETE FROM orders WHERE id = ?");
            s.setString(1, id);
            int rs = s.executeUpdate();
            return rs > 0;
        }
    }

    public static boolean createOrder(Order order) throws SQLException {
        Gson gson = new GsonBuilder().create();
        try (Connection connection = Main.getConnection()) {
            PreparedStatement s = connection.prepareStatement("INSERT INTO orders (id, data) VALUES (?, ?)");
            s.setString(1, order.id);
            s.setString(2, gson.toJson(order));
            int rs = s.executeUpdate();
            return rs > 0;
        }
    }

    public static boolean setPayed(Order order) throws SQLException {
        Gson gson = new GsonBuilder().create();
        try (Connection connection = Main.getConnection()) {
            PreparedStatement s = connection.prepareStatement("UPDATE orders SET data = ? WHERE id = ?");
            s.setString(1, gson.toJson(order));
            s.setString(2, order.id);
            int rs = s.executeUpdate();
            return rs > 0;
        }
    }

    public static boolean setPayed(String id, boolean hidden) throws SQLException {
        try (Connection connection = Main.getConnection()) {
            PreparedStatement s = connection.prepareStatement("UPDATE orders SET hidden = ? WHERE id = ?");
            s.setBoolean(1, hidden);
            s.setString(2, id);
            int rs = s.executeUpdate();
            return rs > 0;
        }
    }
}