package de.olyro.pizza.controllers;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import org.eclipse.jetty.http.HttpStatus;

import de.olyro.pizza.Main;
import de.olyro.pizza.models.FaxMessage;
import de.olyro.pizza.models.FaxStatus;
import de.olyro.pizza.models.Order;
import de.olyro.pizza.models.OrderItem;
import de.olyro.pizza.models.OrderMessage;
import io.javalin.http.Context;

public class AdminController {
    public static class SetPayed {
        public final String id;
        public final boolean payed;

        public SetPayed(String id, boolean payed) {
            this.id = id;
            this.payed = payed;
        }
    }

    public static class SetHidden {
        public final String id;
        public final boolean hidden;

        public SetHidden(String id, boolean hidden) {
            this.id = id;
            this.hidden = hidden;
        }
    }

    public static class GetOrder {
        public final String id;

        public GetOrder(String id) {
            this.id = id;
        }
    }

    public static class SendFax {
        public final String name;
        public final String phoneNumber;
        public final String address;

        public SendFax(String name, String phoneNumber, String address) {
            this.name = name;
            this.phoneNumber = phoneNumber;
            this.address = address;
        }
    }

    public static class SendFaxSipgateRequest {
        public final String faxlineId;
        public final String recipient;
        public final String filename;
        public final String base64Content;

        public SendFaxSipgateRequest(String faxlineId, String recipient, String filename, String base64Content) {
            this.faxlineId = faxlineId;
            this.recipient = recipient;
            this.filename = filename;
            this.base64Content = base64Content;
        }
    }

    public static class SendFaxSipgateResponse {
        public final String sessionId;

        public SendFaxSipgateResponse(String sessionId) {
            this.sessionId = sessionId;
        }
    }

    public static class StatusFaxSipgateResponse {
        public final String faxStatusType;

        public StatusFaxSipgateResponse(String faxStatusType) {
            this.faxStatusType = faxStatusType;
        }
    }

    public static Context sendFax(Context ctx) {
        try {
            var input = ctx.bodyAsClass(SendFax.class);
            var orders = Order.getOrders();
            var orderItems = new ArrayList<OrderItem>();
            orders.stream().map(o -> o.items).forEach(orderItems::addAll);
            var bos = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, bos);

            document.open();
            Font font = FontFactory.getFont(FontFactory.HELVETICA, 14, BaseColor.BLACK);

            document.add(new Paragraph(input.name, font));
            input.address.lines().forEach(line -> {
                try {
                    document.add(new Paragraph(line, font));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            var last = new Paragraph(input.phoneNumber, font);
            last.setSpacingAfter(10);
            document.add(last);

            PdfPTable table = new PdfPTable(1);
            table.setWidthPercentage(100);
            orderItems.stream().forEach(oi -> {
                var cell = new PdfPCell(new Phrase(Main.makeOrderItemString(oi, Main.items), font));
                cell.setPadding(5);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                table.addCell(cell);
            });
            document.add(table);
            document.close();
            var pdfData = Base64.getEncoder().encode(bos.toByteArray());

            Gson gson = new GsonBuilder().create();
            var requestBody = gson.toJson(new SendFaxSipgateRequest(System.getProperty(Main.KEY_SIPGATE_FAXLINE),
                    System.getProperty(Main.KEY_SIPGATE_RECIPIENT), "Bestellung.pdf", new String(pdfData)));

            var client = HttpClient.newBuilder().build();
            var request = HttpRequest.newBuilder().uri(new URI("https://api.sipgate.com/v2/sessions/fax"))
                    .POST(BodyPublishers.ofString(requestBody)).header("Content-Type", "application/json")
                    .header("Authorization", basicAuth(System.getProperty(Main.KEY_SIPGATE_USER),
                            System.getProperty(Main.KEY_SIPGATE_PASSWORD)))
                    .build();

            var response = client.send(request, BodyHandlers.ofString());
            if (response.statusCode() == HttpStatus.OK_200) {
                var sessionId = gson.fromJson(response.body(), SendFaxSipgateResponse.class);
                System.out.println(sessionId.sessionId);
                new Thread(() -> {
                    final var maxMillis = 1000 * 60 * 10;
                    final var loopDuration = 1000 * 10;
                    var currentMillis = 0;
                    while (currentMillis < maxMillis) {
                        try {
                            var faxStatusRequest = HttpRequest.newBuilder()
                                    .uri(new URI("https://api.sipgate.com/v2/history/" + sessionId.sessionId))
                                    .header("Content-Type", "application/json")
                                    .header("Authorization", basicAuth(System.getProperty(Main.KEY_SIPGATE_USER),
                                            System.getProperty(Main.KEY_SIPGATE_PASSWORD)))
                                    .build();

                            var faxStatusResponseResponse = client.send(faxStatusRequest, BodyHandlers.ofString());

                            if (faxStatusResponseResponse.statusCode() == HttpStatus.OK_200) {
                                var parsedResponse = gson.fromJson(faxStatusResponseResponse.body(),
                                        StatusFaxSipgateResponse.class);
                                if (parsedResponse.faxStatusType.equals("SENT")) {
                                    Main.broadcastMessage(new FaxMessage(FaxStatus.SENT), gson);
                                    return;
                                } else if (parsedResponse.faxStatusType.equals("FAILED")) {
                                    Main.broadcastMessage(new FaxMessage(FaxStatus.FAILED), gson);
                                    return;
                                } else {
                                    Main.broadcastMessage(new FaxMessage(FaxStatus.PENDING), gson);
                                }
                            }

                            Thread.sleep(loopDuration);
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            currentMillis += loopDuration;
                        }
                    }
                    Main.broadcastMessage(new FaxMessage(FaxStatus.TIMEOUT), gson);
                }).start();
                return ctx.json(true);
            } else {
                ctx.status(500);
                System.out.println(response.statusCode());
                System.out.println(response.body());
                return ctx.json(false);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            ctx.status(500);
            return ctx.json(false);
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(400);
            return ctx.json(false);
        }
    }

    public static Context setPayed(Context ctx) {
        try {
            Gson gson = new GsonBuilder().create();
            var input = ctx.bodyAsClass(SetPayed.class);
            var oldOrder = Order.getOrder(input.id);
            if (oldOrder.isEmpty()) {
                ctx.status(404);
                return ctx.json(false);
            } else {
                Order.setPayed(new Order(oldOrder.get().id, oldOrder.get().name, input.payed, oldOrder.get().items));
                Main.broadcastMessage(new OrderMessage(Order.getOrders()), gson);
                return ctx.json(true);
            }
        } catch (SQLException e) {
            ctx.status(500);
            return ctx.json(false);
        } catch (Exception e) {
            ctx.status(400);
            return ctx.json(false);
        }
    }

    public static Context setHidden(Context ctx) {
        try {
            Gson gson = new GsonBuilder().create();
            var input = ctx.bodyAsClass(SetHidden.class);
            var oldOrder = Order.getOrder(input.id);
            if (oldOrder.isEmpty()) {
                ctx.status(404);
                return ctx.json(false);
            } else {
                Order.setPayed(input.id, input.hidden);
                Main.broadcastMessage(new OrderMessage(Order.getOrders()), gson);
                return ctx.json(true);
            }
        } catch (SQLException e) {
            ctx.status(500);
            return ctx.json(false);
        } catch (Exception e) {
            ctx.status(400);
            return ctx.json(false);
        }
    }

    public static Context getOrders(Context ctx) {
        try {
            return ctx.json(Order.getOrders());
        } catch (SQLException e) {
            ctx.status(500);
            return ctx.json(false);
        }
    }

    public static Context deleteOrder(Context ctx) {
        try {
            Gson gson = new GsonBuilder().create();
            var input = ctx.bodyAsClass(GetOrder.class);
            var oldOrder = Order.getOrder(input.id);
            if (oldOrder.isEmpty()) {
                ctx.status(404);
                return ctx.json(false);
            } else {
                Order.deleteOrder(input.id);
                Main.broadcastMessage(new OrderMessage(Order.getOrders()), gson);
                return ctx.json(true);
            }
        } catch (SQLException e) {
            ctx.status(500);
            return ctx.json(false);
        } catch (Exception e) {
            ctx.status(400);
            return ctx.json(false);
        }
    }

    private static String basicAuth(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }
}