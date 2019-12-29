package de.olyro.pizza;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stripe.Stripe;

import org.eclipse.jetty.http.HttpStatus;

import de.olyro.pizza.controllers.AdminController;
import de.olyro.pizza.controllers.PayController;
import de.olyro.pizza.models.Item;
import de.olyro.pizza.models.MenuItem;
import de.olyro.pizza.models.Message;
import de.olyro.pizza.models.Order;
import de.olyro.pizza.models.OrderItem;
import de.olyro.pizza.models.OrderMessage;
import de.olyro.pizza.models.Size;
import de.olyro.pizza.models.UserRole;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import io.javalin.plugin.json.JavalinJson;
import io.javalin.websocket.WsContext;

import static io.javalin.core.security.SecurityUtil.roles;

public class Main {
    public static final String KEY_SIPGATE_USER = "KEY_SIPGATE_USER";
    public static final String KEY_SIPGATE_PASSWORD = "KEY_SIPGATE_PASSWORD";
    public static final String KEY_SIPGATE_FAXLINE = "KEY_SIPGATE_FAXLINE";
    public static final String KEY_SIPGATE_RECIPIENT = "KEY_SIPGATE_RECIPIENT";
    public static final String KEY_SECRET = "KEY_SECRET";
    public static final String KEY_STRIPE_SECRET = "KEY_STRIPE_SECRET";
    public static final String KEY_DOMAIN = "KEY_DOMAIN";

    private static Set<WsContext> wsessions = ConcurrentHashMap.newKeySet();
    private static final String secretKey = System.getProperty(KEY_SECRET, "secret");
    public static final List<Item> extras = Arrays
            .asList(new Item("e1", "Tomaten", Arrays.asList(new Size("klein", 50))));
    public static final List<MenuItem> items = Arrays.asList(
            new MenuItem(new Item("p1", "Pizza Seafood mit viel Garnelen", Arrays.asList(new Size("klein", 300))),
                    extras),
            new MenuItem(new Item("p2", "Pizza Salami", Arrays.asList(new Size("klein", 400))), extras));

    public static void main(String[] args) throws SQLException {
        initDB();

        Stripe.apiKey = System.getProperty(KEY_STRIPE_SECRET);

        var app = Javalin.create(config -> {
            config.addStaticFiles("./public", Location.EXTERNAL);
            config.accessManager((handler, ctx, permittedRoles) -> {
                UserRole userRole = getUserRole(ctx);
                if (permittedRoles.contains(userRole)) {
                    handler.handle(ctx);
                } else {
                    ctx.status(401).result("Unauthorized");
                }
            });
        }).start(7000);
        Gson gson = new GsonBuilder().create();
        JavalinJson.setFromJsonMapper(gson::fromJson);
        JavalinJson.setToJsonMapper(gson::toJson);
        app.get("/", ctx -> {
            var data = new HashMap<String, Object>();
            data.put("items",
                    Order.getOrders().stream()
                            .map(order -> Map.of("name", order.name, "content", makeOrderString(order, items), "price",
                                    makePriceString(calcPrice(order, items)), "payed", order.payed ? "ja" : "nein",
                                    "class", order.payed ? "payed" : ""))
                            .collect(Collectors.toList()));
            ctx.render("public/index.mustache", data);
        }, roles(UserRole.ANYONE));
        app.post("/order", ctx -> {
            var id = UUID.randomUUID().toString();
            if (ctx.contentType().contains("json")) {
                try {
                    var orginalOrder = ctx.bodyAsClass(Order.class);
                    var order = new Order(id, orginalOrder.name, false, orginalOrder.items);
                    Order.createOrder(order);
                    broadcastMessage(new OrderMessage(Order.getOrders()), gson);
                    ctx.json(order);
                } catch (Exception e) {
                    e.printStackTrace();
                    ctx.status(HttpStatus.BAD_REQUEST_400);
                    ctx.json(false);
                }
            } else {
                var name = ctx.formParam("name");
                var item = ctx.formParam("item");
                var size = ctx.formParam("size");
                MenuItem mi = items.stream().filter(it -> it.item.id.equals(item)).findFirst().get();
                var ex = mi.extras.stream().filter(extra -> ctx.formParam(extra.id) != null).map(extra -> extra.id)
                        .collect(Collectors.toList());
                var order = new Order(id, name, false, Arrays.asList(new OrderItem(item, size, ex)));
                Order.createOrder(order);
                broadcastMessage(new OrderMessage(Order.getOrders()), gson);
                ctx.redirect("/myorder/" + id);
            }
        }, roles(UserRole.ANYONE));
        app.get("/myorder/:id", ctx -> {
            var order = Order.getOrder(ctx.pathParam("id"));
            if (order.isPresent()) {
                var data = new HashMap<String, Object>();
                data.put("name", order.get().name);
                data.put("content", makeOrderString(order.get(), items));
                data.put("payed", order.get().payed ? "Ja" : "Nein");
                data.put("price", makePriceString(calcPrice(order.get(), items)));
                data.put("id", order.get().id);
                if (!order.get().payed) {
                    data.put("showPayment", "show");
                }
                ctx.render("public/myorder.mustache", data);
            }
        }, roles(UserRole.ANYONE));
        app.get("/menu", ctx -> {
            ctx.json(items);
        }, roles(UserRole.ANYONE));
        app.get("/order", ctx -> {
            var data = new HashMap<String, Object>();
            data.put("items", items);
            var selectedItemId = ctx.queryParam("item");
            if (selectedItemId != null) {
                MenuItem selectedItem = items.stream().filter(item -> item.item.id.equals(selectedItemId)).findFirst()
                        .get();
                data.put("selectedItem", selectedItem);
            }
            ctx.render("public/order.mustache", data);
        }, roles(UserRole.ANYONE));
        app.get("/health", ctx -> ctx.json(true), roles(UserRole.ANYONE));

        app.get("/admin", ctx -> ctx.render("public/admin.mustache"), roles(UserRole.ADMIN));
        app.post("/admin/setPayed", AdminController::setPayed, roles(UserRole.ADMIN));
        app.post("/admin/setHidden", AdminController::setHidden, roles(UserRole.ADMIN));
        app.post("/admin/orders", AdminController::getOrders, roles(UserRole.ADMIN));
        app.post("/admin/deleteOrder", AdminController::deleteOrder, roles(UserRole.ADMIN));
        app.post("/admin/sendFax", AdminController::sendFax, roles(UserRole.ADMIN));

        app.get("/pay/giropay/:id", PayController::payWithGiropay, roles(UserRole.ANYONE));
        app.get("/pay/giropay/processing/:id", PayController::payWithGiropayProcessing, roles(UserRole.ANYONE));

        app.ws("/websocket", ws -> {
            ws.onConnect(ctx -> {
                wsessions.add(ctx);
            });

            ws.onClose(ctx -> {
                wsessions.remove(ctx);
            });
        }, roles(UserRole.ADMIN));
    }

    private static UserRole getUserRole(Context ctx) {
        var key = ctx.queryParam("key");
        if (key != null && key.equals(secretKey)) {
            return UserRole.ADMIN;
        } else {
            return UserRole.ANYONE;
        }
    }

    public static void broadcastMessage(Message message, Gson gson) {
        wsessions.stream().filter(ctx -> ctx.session.isOpen()).forEach(session -> {
            session.send(gson.toJson(message));
        });
    }

    public static String makeOrderItemString(OrderItem item, List<MenuItem> items) {
        MenuItem mi = items.stream().filter(it -> it.item.id.equals(item.id)).findFirst().get();
        var result = mi.item.name + " (" + item.size + ") ";
        if (item.extraIds.size() > 0) {
            result += "(";
            result += item.extraIds.stream()
                    .map(extraId -> "+" + mi.extras.stream().filter(e -> e.id.equals(extraId)).findFirst().get().name)
                    .collect(Collectors.joining(", "));
            result += ")";
        }
        return result;
    }

    private static String makeOrderString(Order o, List<MenuItem> items) {
        return o.items.stream().map(item -> makeOrderItemString(item, items)).collect(Collectors.joining(", "));
    }

    private static String makePriceString(int cents) {
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        return currencyFormatter.format(cents / 100.0);
    }

    public static int calcPrice(Order o, List<MenuItem> items) {
        return o.items.stream().map(item -> {
            var result = 0;
            MenuItem mi = items.stream().filter(it -> it.item.id.equals(item.id)).findFirst().get();
            var size = mi.item.sizes.stream().filter(s -> s.name.equals(item.size)).findFirst().get();
            result += size.price;
            var extraPrice = item.extraIds.stream()
                    .map(extraId -> mi.extras.stream().filter(extra -> extra.id.equals(extraId)).findFirst().get().sizes
                            .stream().filter(s -> s.name.equals(size.name)).findFirst().get().price)
                    .reduce((p1, p2) -> p1 + p2).orElse(0);
            result += extraPrice;
            return result;
        }).reduce((p1, p2) -> p1 + p2).orElse(0);
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:db/orders.db");
    }

    public static void initDB() throws SQLException {
        try (Connection connection = getConnection()) {
            PreparedStatement s = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS orders (id text PRIMARY KEY, hidden BOOLEAN DEFAULT FALSE, date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, data TEXT NOT NULL);");
            s.execute();
        }
    }
}