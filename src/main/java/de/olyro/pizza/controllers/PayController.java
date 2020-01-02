package de.olyro.pizza.controllers;

import java.sql.SQLException;
import java.util.HashMap;

import com.google.gson.GsonBuilder;
import com.stripe.model.Charge;
import com.stripe.model.Source;

import org.eclipse.jetty.http.HttpStatus;

import de.olyro.pizza.Main;
import de.olyro.pizza.models.Order;
import de.olyro.pizza.models.OrderMessage;
import io.javalin.http.Context;

public class PayController {
    public static Context payWithGiropay(Context ctx) {
        try {
            var id = ctx.pathParam("id");
            var order = Order.getOrder(id);
            if (order.isPresent() && !order.get().payed) {
                var sourceParams = new HashMap<String, Object>();
                sourceParams.put("type", "giropay");
                sourceParams.put("currency", "eur");
                sourceParams.put("amount", Main.calcPrice(order.get(), Main.items));
                var ownerParams = new HashMap<String, Object>();
                ownerParams.put("email", "christoph@raytracer.me");
                ownerParams.put("name", "Christoph Müller");
                sourceParams.put("owner", ownerParams);

                var redirectParams = new HashMap<String, Object>();
                redirectParams.put("return_url",
                        System.getProperty(Main.KEY_DOMAIN) + "/pay/giropay/processing/" + order.get().id);

                sourceParams.put("redirect", redirectParams);

                var src = Source.create(sourceParams);
                ctx.redirect(src.getRedirect().getUrl());
                return ctx;
            } else {
                ctx.status(HttpStatus.NOT_FOUND_404);
                return ctx.html("Bestellung nicht gefunden");
            }
        } catch (SQLException e) {
            ctx.status(HttpStatus.BAD_REQUEST_400);
            return ctx.render("Datenbankfehler");
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(HttpStatus.BAD_REQUEST_400);
            return ctx.html("Keine Id");
        }
    }

    public static Context payWithGiropayProcessing(Context ctx) {
        try {
            var id = ctx.pathParam("id");
            var order = Order.getOrder(id);
            if (order.isPresent() && !order.get().payed) {
                var params = new HashMap<String, Object>();
                params.put("amount", Main.calcPrice(order.get(), Main.items));
                params.put("currency", "eur");
                params.put("source", ctx.queryParam("source"));

                var charge = Charge.create(params);
                var status = charge.getRawJsonObject().get("status").getAsString();

                // stripe webhooks would probably better, but for this simple application
                // polling is sufficient
                // since giropy is synchronous
                while (!(status.equals("succeeded") || status.equals("failed"))) {
                    charge = Charge.retrieve(charge.getId());
                    status = charge.getRawJsonObject().get("status").getAsString();
                    Thread.sleep(100);
                }

                if (status.equals("succeeded")) {
                    Order.setPayed(new Order(order.get().id, order.get().name, true, order.get().items));
                    Main.broadcastMessage(new OrderMessage(Order.getOrders()), (new GsonBuilder()).create());
                }

                var data = new HashMap<String, Object>();
                data.put("status", status.equals("succeeded") ? "Zahlung erfoglreich" : "Zahlung fehlgeschlagen");
                data.put("link", "/myorder/" + id);
                return ctx.render("public/payment.mustache", data);
            } else {
                ctx.status(HttpStatus.NOT_FOUND_404);
                return ctx.html("Bestellung nicht gefunden");
            }
        } catch (SQLException e) {
            ctx.status(HttpStatus.BAD_REQUEST_400);
            return ctx.render("Datenbankfehler");
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST_400);
            return ctx.html("Keine Id");
        }
    }
}