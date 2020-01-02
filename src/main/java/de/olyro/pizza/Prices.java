package de.olyro.pizza;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import de.olyro.pizza.models.Item;
import de.olyro.pizza.models.MenuItem;
import de.olyro.pizza.models.Size;

public class Prices {
        final static private String s = "klein";
        final static private String l = "groß";
        final static private String f = "Family, 45x32 cm";
        final static private String p = "Party, 60x40 cm";

        public static List<Item> getExtraItems() {
                var es = Arrays.asList(new Size(s, 50), new Size(l, 60), new Size(f, 110), new Size(p, 180));
                var extras = Arrays.asList(new Item("e1", "Basilikum", es), new Item("e2", "Knoblauch", es),
                                new Item("e3", "Oregano", es), new Item("e4", "Ananas", es),
                                new Item("e5", "Artischockenherzen", es), new Item("e6", "Bacon", es),
                                new Item("e7", "Barbecuesauce", es), new Item("e8", "Blattspinat", es),
                                new Item("e9", "Brokkoli", es), new Item("e10", "Champignons", es),
                                new Item("e11", "Eier", es), new Item("e12", "Fetakäse", es),
                                new Item("e13", "Frutti di Mare", es), new Item("e14", "Gorgonzola", es),
                                new Item("e15", "Gouda", es), new Item("e16", "Hühnerbrust", es),
                                new Item("e17", "Kapern", es), new Item("e18", "Mais", es),
                                new Item("e19", "Mozzarella", es), new Item("e20", "Olivenscheiben", es),
                                new Item("e21", "Paprika", es), new Item("e22", "Peperoniringe", es),
                                new Item("e23", "Peperoniwurst", es), new Item("e24", "Remoulade", es),
                                new Item("e25", "Salami", es), new Item("e26", "Salsa", es),
                                new Item("e27", "Sardellen", es), new Item("e28", "Sauce Bolognese", es),
                                new Item("e29", "Shrimps", es), new Item("e30", "Spargel", es),
                                new Item("e31", "Tabasco", es), new Item("e32", "Taco Beef", es),
                                new Item("e33", "Thunfisch", es), new Item("e34", "Zwiebelringe", es),
                                new Item("e35", "frische Champignons", es),
                                new Item("e36", "frische Tomatenscheiben", es),
                                new Item("e37", "italienischer Vorderschinken", es),
                                new Item("e38", "mexikanische Jalapeno", es));
                return extras;
        }

        public static List<MenuItem> getOriginalMenuItems() {
                var extras = getExtraItems();
                var pizzas = Arrays.asList(
                                new MenuItem(new Item("p1", "Pizza Margherita",
                                                Arrays.asList(new Size(s, 650), new Size(l, 840), new Size(f, 1440),
                                                                new Size(p, 1790))),
                                                "mit Pizzasauce, Mozzarella und Basilikum", extras),
                                new MenuItem(new Item("p2", "Pizza New York",
                                                Arrays.asList(new Size(s, 700), new Size(l, 900), new Size(f, 1550),
                                                                new Size(p, 1970))),
                                                "mit Hühnerbrust und Oliven", extras),
                                new MenuItem(new Item("p3", "Pizza Popeye",
                                                Arrays.asList(new Size(s, 700), new Size(l, 900), new Size(f, 1550),
                                                                new Size(p, 1970))),
                                                "mit Spinat und Feta", extras),
                                new MenuItem(new Item("p4", "Pizza Hawaii",
                                                Arrays.asList(new Size(s, 700), new Size(l, 900), new Size(f, 1550),
                                                                new Size(p, 1970))),
                                                "mit Schinken und Ananas", extras),
                                new MenuItem(new Item("p5", "Pizza Mary",
                                                Arrays.asList(new Size(s, 750), new Size(l, 960), new Size(f, 1660),
                                                                new Size(p, 2150))),
                                                "mit Schinken, Salami und Champignons", extras),
                                new MenuItem(new Item("p6", "Pizza Samoa",
                                                Arrays.asList(new Size(s, 750), new Size(l, 960), new Size(f, 1660),
                                                                new Size(p, 2150))),
                                                "mit Thunfisch, Zwiebelringen und Ananas", extras),
                                new MenuItem(new Item("p7", "Pizza Texas",
                                                Arrays.asList(new Size(s, 750), new Size(l, 960), new Size(f, 1660),
                                                                new Size(p, 2150))),
                                                "mit Taco Beef, Jalapenos und Bohnen", extras),
                                new MenuItem(new Item("p8", "Pizza Jazz",
                                                Arrays.asList(new Size(s, 800), new Size(l, 1020), new Size(f, 1770),
                                                                new Size(p, 2330))),
                                                "mit Schinken, Spargel, Tomaten und Barbecuesauce", extras),
                                new MenuItem(new Item("p9", "Pizza Veggie",
                                                Arrays.asList(new Size(s, 800), new Size(l, 1020), new Size(f, 1770),
                                                                new Size(p, 2330))),
                                                "mit Broccoli, Tomaten, Paprika und Artischocken", extras),
                                new MenuItem(new Item("p10", "Pizza Capricciosa",
                                                Arrays.asList(new Size(s, 850), new Size(l, 1080), new Size(f, 1880),
                                                                new Size(p, 2510))),
                                                "mit Schinken, Salami, Oliven, Paprika und Zwiebeln", extras),
                                new MenuItem(new Item("p11", "Pizza Mexicana",
                                                Arrays.asList(new Size(s, 850), new Size(l, 1080), new Size(f, 1880),
                                                                new Size(p, 2510))),
                                                "mit Peperoniwurst, Speck, Taco Beef, Jalapenos und Zwiebeln", extras),
                                new MenuItem(new Item("p12", "Pizza Outback",
                                                Arrays.asList(new Size(s, 850), new Size(l, 1080), new Size(f, 1880),
                                                                new Size(p, 2510))),
                                                "mit Taco Beef, Schinken, Zwiebeln, Jalapenos und Ei", extras),
                                new MenuItem(new Item("p13", "Pizza Beverly Hills",
                                                Arrays.asList(new Size(s, 850), new Size(l, 1080), new Size(f, 1880),
                                                                new Size(p, 2510))),
                                                "mit Hühnerbrust, Taco Beef, Broccoli, Ananas und Barbecuesauce",
                                                extras),
                                new MenuItem(new Item("p14", "Pizza Speciale",
                                                Arrays.asList(new Size(s, 850), new Size(l, 1080), new Size(f, 1880),
                                                                new Size(p, 2510))),
                                                "mit Schinken, Salami, Champignons, Paprika und Ei", extras),
                                new MenuItem(new Item("p15", "Pizza Full House",
                                                Arrays.asList(new Size(s, 900), new Size(l, 1140), new Size(f, 1990),
                                                                new Size(p, 2690))),
                                                "mit Schinken, Peperoniwurst, Speck, Paprika, Peperoni und Ei", extras),
                                new MenuItem(new Item("p16", "Pizza Basic/Wunschpizza",
                                                Arrays.asList(new Size(s, 600), new Size(l, 780), new Size(f, 1310),
                                                                new Size(p, 1610))),
                                                "mit Pizzasauce und Käse", extras));

                return pizzas;
        }

        public static List<MenuItem> getTransformedMenuItems() {
                return getOriginalMenuItems().stream().map(Prices::transformMenuItem).collect(Collectors.toList());
        }

        public static MenuItem transformMenuItem(MenuItem menuItem) {
                return new MenuItem(
                                new Item(menuItem.item.id, menuItem.item.name,
                                                menuItem.item.sizes.stream().map(
                                                                size -> new Size(size.name, transformPrice(size.price)))
                                                                .collect(Collectors.toList())),
                                menuItem.description, menuItem.extras);
        }

        private static int transformPrice(int cents) {
                // includes tip + Pizza for the lecturer + Stripe fees + Sipgate fax costs
                var withSurcharge = Math.round(cents * 1.10f);
                return withSurcharge + ((10 - withSurcharge % 10) % 10);
        }
}