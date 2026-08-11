package com.travelapp.catalog;

import com.travelapp.entity.Destination;
import com.travelapp.entity.MenuItem;
import com.travelapp.entity.Restaurant;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class RestaurantCatalogGenerator {

    public static final int PAGE_SIZE = 15;
    public static final int TOTAL_RESTAURANTS = 15;
    public static final int TOTAL_MENU_ITEMS = 15;

    private RestaurantCatalogGenerator() {}

    private static final String[][] INDIAN_RESTAURANTS = {
            {"Paradise Biryani", "Hyderabadi"},
            {"Bawarchi", "Andhra"},
            {"Chutneys", "South Indian"},
            {"Ohri's 360°", "Multi-cuisine"},
            {"Minerva Coffee Shop", "South Indian"},
            {"Spicy Venue", "Andhra"},
            {"Rayalaseema Ruchulu", "Rayalaseema"},
            {"Pista House", "Mughlai"},
            {"Sarvi Restaurant", "Biryani"},
            {"Tatva", "Vegetarian"},
            {"Exotica", "North Indian"},
            {"Platform 65", "Themed"},
            {"Farzi Cafe", "Modern Indian"},
            {"Conçu", "Continental"},
            {"Tiger Lily", "Asian"},
            {"Hoppery", "Continental"},
            {"Ironhill Brewery", "Brewery"},
            {"AB's Absolute Barbecues", "BBQ"},
            {"Barbeque Nation", "BBQ"},
            {"Mainland China", "Chinese"},
            {"Haiku", "Japanese"},
            {"Hard Rock Cafe", "American"},
            {"McDonald's", "Fast Food"},
            {"Domino's", "Pizza"},
            {"Subway", "Sandwiches"},
            {"KFC", "Fast Food"},
            {"Pizza Hut", "Pizza"},
            {"Cafe Coffee Day", "Cafe"},
            {"Starbucks", "Cafe"},
            {"Haldiram's", "Snacks"},
            {"Karachi Bakery", "Bakery"},
            {"Nimrah Cafe", "Bakery"},
            {"Shah Ghouse", "Biryani"},
            {"Mehfil", "Biryani"},
            {"Grand Hotel", "Andhra"},
            {"Kakatiya Mess", "Telangana"},
            {"Ulavacharu", "Coastal"},
            {"Spice 6", "Multi-cuisine"},
            {"Jewel of Nizam", "Fine Dining"},
            {"Flechazo", "Mediterranean"},
            {"Prost", "Brewery"},
            {"Dockyard", "Seafood"},
            {"Trident Lounge", "Fine Dining"},
            {"Little Italy", "Italian"},
            {"SodaBottleOpenerWala", "Parsi"}
    };

    private static final String[][] INDIAN_DISHES = {
            {"Chicken Dum Biryani", "Slow-cooked aromatic biryani", "450"},
            {"Mutton Biryani", "Royal Nizami style", "520"},
            {"Paneer Butter Masala", "Creamy tomato gravy", "320"},
            {"Hyderabadi Haleem", "Seasonal delicacy", "280"},
            {"Double Ka Meetha", "Classic dessert", "180"},
            {"Gongura Mutton", "Andhra specialty", "410"},
            {"Pesarattu", "Green gram dosa", "120"},
            {"Filter Coffee", "South Indian brew", "60"},
            {"Apollo Fish", "Spicy starter", "340"},
            {"Chicken 65", "Crispy fried chicken", "290"},
            {"Dahi Puri", "Street chaat", "90"},
            {"Masala Dosa", "Crisp rice crepe", "110"},
            {"Idli Sambar", "Steamed rice cakes", "80"},
            {"Veg Manchurian", "Indo-Chinese favorite", "240"},
            {"Hakka Noodles", "Stir-fried noodles", "220"},
            {"Margherita Pizza", "Wood-fired classic", "350"},
            {"BBQ Chicken Wings", "Smoky glazed wings", "380"},
            {"Chocolate Brownie", "Warm with ice cream", "200"},
            {"Fresh Lime Soda", "Refreshing cooler", "70"},
            {"Mango Lassi", "Seasonal yogurt drink", "90"},
            {"Tandoori Roti", "Clay oven bread", "40"},
            {"Dal Tadka", "Yellow lentil curry", "180"},
            {"Jeera Rice", "Cumin tempered rice", "150"},
            {"Fish Fry", "Coastal spiced fry", "360"},
            {"Prawn Curry", "Coconut based gravy", "440"},
            {"Veg Thali", "Complete meal platter", "280"},
            {"Non-Veg Thali", "Andhra style platter", "380"},
            {"Keema Pav", "Spiced minced meat", "260"},
            {"Butter Naan", "Soft leavened bread", "50"},
            {"Rasmalai", "Bengali sweet", "120"},
            {"Gulab Jamun", "Syrup-soaked sweet", "80"},
            {"Cold Coffee", "Blended iced coffee", "110"},
            {"Peri Peri Fries", "Spicy fries", "140"},
            {"Caesar Salad", "Fresh greens", "220"},
            {"Grilled Sandwich", "Cheese & veggies", "160"},
            {"Egg Burji", "Scrambled eggs", "130"},
            {"Chicken Shawarma", "Arabian wrap", "210"},
            {"Falooda", "Layered dessert drink", "150"},
            {"Irani Chai", "Strong milk tea", "50"},
            {"Osmania Biscuit", "Local tea biscuit", "30"},
            {"Mirchi Bajji", "Stuffed chili fritter", "60"},
            {"Samosa", "Crispy potato pastry", "40"},
            {"Kathi Roll", "Kolkata style wrap", "190"},
            {"Szechwan Fried Rice", "Spicy rice", "230"},
            {"Butter Chicken", "North Indian classic", "390"}
    };

    public static List<Restaurant> generateRestaurants(Destination destination) {
        Random random = new Random(seed(destination.getCity() + destination.getCountry()));
        List<Restaurant> list = new ArrayList<>();
        String city = destination.getCity() != null ? destination.getCity() : destination.getName();

        String[][] localRestaurants;
        if (city.toLowerCase().contains("warangal")) {
            localRestaurants = new String[][]{
                {"Kakatiya Biryani & Kebabs", "Telangana Traditional"},
                {"Bhadrakali Tiffins", "South Indian"},
                {"Telangana Spice Kitchen", "Regional Indian"},
                {"Thousand Pillar Food Court", "Street Food"},
                {"Hanamkonda Grand Mess", "Andhra & Telangana"},
                {"Orugallu Heritage Restaurant", "Local Delicacies"},
                {"Warangal Biryani Point", "Biryani"},
                {"Reddy Gari Mess", "South Indian Meals"},
                {"Fort City Bakers", "Bakery & Cafe"},
                {"Ramappa Lake View Dhaba", "Andhra Dhaba"}
            };
        } else if (city.toLowerCase().contains("araku")) {
            localRestaurants = new String[][]{
                {"Araku Coffee House & Bakery", "Cafe"},
                {"Tribal Spice Restaurant", "Tribal Food"},
                {"Valley View Dhaba", "Andhra Regional"},
                {"Bamboo Chicken Hub", "Local Specialty"},
                {"Araku Garden Restaurant", "Multi-cuisine"},
                {"Hill Station Bistro", "Continental"},
                {"Coffee Plantation Cafe", "Cafe & Snacks"},
                {"Giri Ruchulu Mess", "Andhra Meals"},
                {"Araku Heritage Inn", "Regional Indian"},
                {"Padmapuram Garden Dining", "Vegetarian"}
            };
        } else if (city.toLowerCase().contains("goa")) {
            localRestaurants = new String[][]{
                {"Fisherman's Wharf", "Goan Seafood"},
                {"Britto's Beach Shack", "Seafood & Grill"},
                {"Martin's Corner", "Traditional Goan"},
                {"Curlies Beach Shack", "Multi-cuisine"},
                {"Thalassa Goan Greek", "Greek & Seafood"},
                {"Vinayak Family Restaurant", "Goan Fish Thali"},
                {"Souza Lobo", "Coastal Seafood"},
                {"Mum's Kitchen", "Traditional Goan"},
                {"Gunpowder", "South Indian Coastal"},
                {"Goan Portuguese Bistro", "Goan Portuguese"}
            };
        } else if (city.toLowerCase().contains("mumbai")) {
            localRestaurants = new String[][]{
                {"Leopold Cafe & Bar", "Irani Cafe"},
                {"Elco Pani Puri Center", "Mumbai Street Food"},
                {"Trishna Seafood Restaurant", "Coastal Seafood"},
                {"Britannia & Co. Restaurant", "Parsi Cuisine"},
                {"Bademiya Kebabs", "Mughlai & Kebabs"},
                {"Mahesh Lunch Home", "Mangalorean Seafood"},
                {"Kyani & Co. Bakery", "Bakery & Cafe"},
                {"Gajalee Coastal Curry", "Malvani Seafood"},
                {"The Bombay Canteen", "Modern Indian"},
                {"Shiv Sagar Veg Court", "South Indian & Chaat"}
            };
        } else if (city.toLowerCase().contains("delhi")) {
            localRestaurants = new String[][]{
                {"Karim's Old Delhi", "Mughlai"},
                {"Paranthe Wali Gali", "North Indian"},
                {"Indian Accent", "Modern Indian"},
                {"Bukhara ITC Maurya", "Tandoori & Grill"},
                {"Saravana Bhavan", "South Indian"},
                {"Moti Mahal Delux", "North Indian"},
                {"Kuremal Mohan Lal Kulfi", "Desserts"},
                {"Gulati Restaurant", "North Indian Mughlai"},
                {"Wenger's Bakery", "Bakery & Cafe"},
                {"Natraj Dahi Bhalla Corner", "Street Food"}
            };
        } else {
            localRestaurants = new String[][]{
                {city + " Bistro & Cafe", "Local Cafe"},
                {city + " Royal Palace", "Fine Dining"},
                {city + " Heritage Kitchen", "Traditional Cuisine"},
                {city + " Street Food Corner", "Street Food"},
                {city + " Valley View Grill", "Barbecue & Grill"},
                {city + " Central Eatery", "Multi-cuisine"},
                {city + " Lakeside Inn", "Coastal & Seafood"},
                {city + " Boutique Table", "Modern Fusion"},
                {city + " Garden Pizzeria", "Italian Pizza"},
                {city + " Express Tiffins", "Fast Food"}
            };
        }

        for (int i = 0; i < TOTAL_RESTAURANTS; i++) {
            String name;
            String cuisine;
            int templateIndex = i % localRestaurants.length;
            
            if (i < localRestaurants.length) {
                name = localRestaurants[i][0];
                cuisine = localRestaurants[i][1];
            } else {
                name = localRestaurants[templateIndex][0] + " " + (i / localRestaurants.length + 1);
                cuisine = localRestaurants[templateIndex][1];
            }

            list.add(Restaurant.builder()
                    .destination(destination)
                    .name(name)
                    .cuisine(cuisine)
                    .rating(round(3.8 + random.nextDouble() * 1.1))
                    .deliveryMinutes(20 + random.nextInt(35))
                    .costForTwo(300 + random.nextInt(1200))
                    .imageUrl(foodImage(getSignatureDish(name), i))
                    .rankOrder(i + 1)
                    .build());
        }
        return list;
    }

    public static List<MenuItem> generateMenuItems(Restaurant restaurant) {
        Random random = new Random(seed(restaurant.getName() + restaurant.getId()));
        List<MenuItem> items = new ArrayList<>();
        String restName = restaurant.getName().toLowerCase();
        String cuisine = restaurant.getCuisine().toLowerCase();

        List<String[]> dishPool = new ArrayList<>();
        
        if (cuisine.contains("telangana") || restName.contains("kakatiya") || restName.contains("warangal") || restName.contains("orugallu")) {
            dishPool.add(new String[]{"Telangana Mutton Curry", "Spicy traditional mutton curry", "380"});
            dishPool.add(new String[]{"Kakatiya Chicken Biryani", "Fragrant spicy local biryani", "320"});
            dishPool.add(new String[]{"Sarvapindi", "Rice flour spicy flatbread", "90"});
            dishPool.add(new String[]{"Natukodi Pulusu", "Country chicken spicy gravy", "360"});
            dishPool.add(new String[]{"Boti Fry", "Spiced fried goat tripe", "240"});
            dishPool.add(new String[]{"Sajjalu Rotte", "Pearl millet flatbread", "60"});
        } else if (cuisine.contains("tribal") || restName.contains("araku") || cuisine.contains("bamboo")) {
            dishPool.add(new String[]{"Araku Bamboo Chicken", "Chicken cooked inside bamboo shoots without oil", "350"});
            dishPool.add(new String[]{"Organic Araku Coffee", "Freshly brewed local organic coffee", "90"});
            dishPool.add(new String[]{"Bamboo Shoot Curry", "Traditional spicy tribal veg curry", "210"});
            dishPool.add(new String[]{"Araku Valley Honey Tea", "Fresh forest honey and green tea", "80"});
            dishPool.add(new String[]{"Ragi Mudda with Chicken", "Finger millet balls with country chicken curry", "240"});
        } else if (cuisine.contains("goan") || cuisine.contains("portuguese") || restName.contains("goa") || restName.contains("wharf") || restName.contains("shack") || restName.contains("lobo")) {
            dishPool.add(new String[]{"Goan Fish Curry Rice", "Fresh fish cooked in coconut spicy curry served with rice", "320"});
            dishPool.add(new String[]{"Pork Vindaloo", "Tangy and spicy Portuguese Goan pork dish", "380"});
            dishPool.add(new String[]{"Butter Garlic Prawns", "Juicy prawns sauteed in butter and garlic sauce", "410"});
            dishPool.add(new String[]{"Chicken Xacuti", "Rich coconut and spice gravy chicken", "340"});
            dishPool.add(new String[]{"Goan Bebinca", "Traditional multi-layered Goan coconut dessert", "180"});
            dishPool.add(new String[]{"Goan Serradura", "Sawdust pudding dessert", "140"});
        } else if (cuisine.contains("mumbai") || restName.contains("mumbai") || restName.contains("bombay") || restName.contains("leopold") || restName.contains("elco")) {
            dishPool.add(new String[]{"Vada Pav Duo", "The classic Mumbai potato burger served with chutneys", "60"});
            dishPool.add(new String[]{"Special Pav Bhaji", "Spiced mashed vegetables served with butter toasted pav", "140"});
            dishPool.add(new String[]{"Keema Pav", "Spiced minced mutton served with soft butter buns", "240"});
            dishPool.add(new String[]{"Mumbai Sev Puri", "Crispy puris topped with potatoes, onions, and chutneys", "90"});
            dishPool.add(new String[]{"Irani Bun Maska Chai", "Soft buttered bun with warm Irani chai", "80"});
            dishPool.add(new String[]{"Bombay Veg Sandwich", "Classic mint chutney and vegetable sandwich", "110"});
        } else if (cuisine.contains("mughlai") || restName.contains("delhi") || restName.contains("karim") || restName.contains("paranthe") || restName.contains("bukhara")) {
            dishPool.add(new String[]{"Jama Masjid Mutton Korma", "Rich gravy mutton cooked with traditional spices", "390"});
            dishPool.add(new String[]{"Stuffed Aloo Parantha", "Deep-fried crisp parantha with pickle and butter", "110"});
            dishPool.add(new String[]{"Delhi Butter Chicken", "Creamy, rich tomato-butter gravy chicken", "360"});
            dishPool.add(new String[]{"Chicken Seekh Kebab", "Charcoal-grilled spiced minced chicken skewers", "280"});
            dishPool.add(new String[]{"Delhi Rabri Jalebi", "Hot crispy jalebis with thick sweet rabri", "150"});
            dishPool.add(new String[]{"Chandni Chowk Chole Bhature", "Spicy chickpeas with large fluffy fried breads", "160"});
        }

        for (String[] dish : INDIAN_DISHES) {
            dishPool.add(dish);
        }

        for (int i = 0; i < TOTAL_MENU_ITEMS; i++) {
            int index = i % dishPool.size();
            String dishName = dishPool.get(index)[0];
            String desc = dishPool.get(index)[1];
            double price = Double.parseDouble(dishPool.get(index)[2]);
            
            boolean veg = dishName.toLowerCase().contains("paneer")
                    || dishName.toLowerCase().contains("veg")
                    || dishName.toLowerCase().contains("dosa")
                    || dishName.toLowerCase().contains("idli")
                    || dishName.toLowerCase().contains("dal")
                    || dishName.toLowerCase().contains("coffee")
                    || dishName.toLowerCase().contains("tea")
                    || dishName.toLowerCase().contains("sweet")
                    || dishName.toLowerCase().contains("jalebi")
                    || dishName.toLowerCase().contains("pizza")
                    || dishName.toLowerCase().contains("pav bhaji")
                    || dishName.toLowerCase().contains("vada pav")
                    || dishName.toLowerCase().contains("chutney");

            items.add(MenuItem.builder()
                    .restaurant(restaurant)
                    .name(dishName)
                    .description(desc)
                    .price(price)
                    .rating(round(3.5 + random.nextDouble() * 1.4))
                    .veg(veg)
                    .category(pick(random, "Main", "Starter", "Dessert", "Beverage"))
                    .imageUrl(foodImage(dishName, i))
                    .sortOrder(i + 1)
                    .build());
        }
        return items;
    }

    private static long seed(String input) {
        return input.toLowerCase().hashCode() & 0xFFFFFFFFL;
    }

    private static double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static String pick(Random r, String... options) {
        return options[r.nextInt(options.length)];
    }

    public static String getSignatureDish(String restaurantName) {
        String r = restaurantName.toLowerCase();
        if (r.contains("biryani") || r.contains("house") || r.contains("mehfil") || r.contains("sarvi")) return "Chicken Dum Biryani";
        if (r.contains("chutneys") || r.contains("minerva")) return "Masala Dosa";
        if (r.contains("sushi") || r.contains("haiku")) return "Sushi";
        if (r.contains("pizza") || r.contains("domino")) return "Margherita Pizza";
        if (r.contains("burger") || r.contains("mcdonald") || r.contains("kfc")) return "Chicken 65";
        if (r.contains("coffee") || r.contains("starbucks")) return "Filter Coffee";
        if (r.contains("bakery")) return "Osmania Biscuit";
        if (r.contains("italy")) return "Margherita Pizza";
        if (r.contains("chinese") || r.contains("mainland")) return "Veg Manchurian";
        if (r.contains("seafood") || r.contains("dockyard")) return "Fish Fry";
        return "Veg Thali";
    }

    private static String foodImage(String name, int index) {
        String n = name.toLowerCase();
        String photoId = "photo-1546069901-ba9599a7e63c"; // default (salad)
        if (n.contains("biryani") || n.contains("haleem")) {
            photoId = "photo-1633945274405-b6c8069047b0";
        } else if (n.contains("dosa") || n.contains("pesarattu") || n.contains("idli")) {
            photoId = "photo-1668236543090-82eba5ee5976";
        } else if (n.contains("pizza")) {
            photoId = "photo-1513104890138-7c749659a591";
        } else if (n.contains("burger") || n.contains("kfc") || n.contains("mcdonald") || n.contains("wings")) {
            photoId = "photo-1568901346375-23c9450c58cd";
        } else if (n.contains("coffee") || n.contains("starbucks") || n.contains("cafe") || n.contains("tea") || n.contains("chai")) {
            photoId = "photo-1507133750040-4a8f57021571";
        } else if (n.contains("sweet") || n.contains("dessert") || n.contains("brownie") || n.contains("meetha") || n.contains("jamun") || n.contains("rasmalai")) {
            photoId = "photo-1606313564200-e75d5e30476c";
        } else if (n.contains("paneer") || n.contains("butter masala") || n.contains("dal") || n.contains("thali")) {
            photoId = "photo-1589301760014-d929f3979dbc";
        } else if (n.contains("fish") || n.contains("prawn") || n.contains("seafood")) {
            photoId = "photo-1519708227418-c8fd9a32b7a2";
        } else if (n.contains("chicken") || n.contains("mutton") || n.contains("shawarma") || n.contains("fry")) {
            photoId = "photo-1606787366850-de6330128bfc";
        } else if (n.contains("samosa") || n.contains("bajji") || n.contains("puri") || n.contains("chaat")) {
            photoId = "photo-1601050690597-df056fb4ce78";
        } else if (n.contains("sandwich") || n.contains("subway")) {
            photoId = "photo-1509722747041-616f39b57569";
        } else if (n.contains("noodles") || n.contains("manchurian") || n.contains("chinese") || n.contains("fried rice")) {
            photoId = "photo-1585032226651-759b368d7246";
        } else {
            String[] pool = {
                "photo-1546069901-ba9599a7e63c",
                "photo-1565299624946-b28f40a0ae38",
                "photo-1555939594-58d7cb561ad1",
                "photo-1484723091739-30a097e8f929",
                "photo-1482049016688-2d3e1b311543",
                "photo-1473093295043-cdd812d0e601"
            };
            photoId = pool[Math.abs(name.hashCode() % pool.length)];
        }
        return "https://images.unsplash.com/" + photoId + "?w=400&q=80";
    }
}
