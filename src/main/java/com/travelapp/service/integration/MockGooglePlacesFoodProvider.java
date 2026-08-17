package com.travelapp.service.integration;

import com.travelapp.entity.Destination;
import com.travelapp.entity.FoodRecommendation;
import com.travelapp.enums.FoodSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MockGooglePlacesFoodProvider implements ThirdPartyFoodProvider {

    private final WikipediaImageClient wikipediaImageClient;

    @Override
    public List<FoodRecommendation> fetchTopRestaurants(Destination dest) {
        String city = dest.getCity() != null ? dest.getCity() : dest.getName();
        String country = dest.getCountry() != null ? dest.getCountry() : "Global";

        List<FoodRecommendation> list = new ArrayList<>();

        String[] spotTypes = {
                "Grand Culinary Palace", "Coastal Seafood Kitchen", "Heritage Spice Bistro", "Rooftop Sunset Lounge",
                "Old Market Street Food Corner", "Artisan Bakery & Cafe", "Royal Banquet Hall", "Wood-Fired Pizzeria & Grill",
                "Lantern Noodle Bar", "Tapas & Bodega House", "Fine Dining Atelier", "Harbor Fisherman's Shack",
                "Traditional Clay Oven Dhaba", "Organic Farm-to-Table Kitchen", "Spice Route Grill", "Midnight Gourmet Diner",
                "The Tea & Pastry Salon", "Smokery & Charcoal BBQ", "Azure Coastal Bistro", "Master Chef's Table"
        };

        String[] cuisines = {
                "Traditional Regional", "Fresh Seafood", "Authentic Local Flavors", "Contemporary Fusion",
                "Street Food Specialties", "Artisan Cafe & Bakery", "Royal Cuisine", "Wood-Fired Italian",
                "Asian Street Noodles", "Spanish Tapas & Wine", "Modern Haute Cuisine", "Coastal Catch",
                "Claypot & Tandoor", "Farm-to-Table Organic", "Charcoal Grill", "Late-Night Comfort Food",
                "High Tea & Desserts", "Smoked Barbecue", "Mediterranean Seafood", "Chef's Signature Tasting"
        };

        String[] priceRanges = {
                "$$$", "$$", "$$", "$$$",
                "$", "$", "$$$$", "$$",
                "$", "$$", "$$$$", "$$",
                "$", "$$", "$$", "$",
                "$$", "$$", "$$$", "$$$$"
        };

        double[] ratings = {4.8, 4.7, 4.9, 4.6, 4.7, 4.8, 4.9, 4.7, 4.6, 4.8, 4.9, 4.7, 4.8, 4.7, 4.8, 4.6, 4.7, 4.8, 4.7, 4.9};

        for (int i = 0; i < 20; i++) {
            String name = (city + " " + spotTypes[i]);
            String desc = "Celebrated top dining destination in " + city + " known for signature " + cuisines[i].toLowerCase() + ".";
            String address = (12 + i * 5) + " Central Road, " + city + ", " + country;
            
            list.add(createRecommendation(dest, name, desc, cuisines[i], ratings[i], priceRanges[i], address));
        }

        return list;
    }

    private FoodRecommendation createRecommendation(Destination dest, String name, String desc, String cuisine, double rating, String price, String address) {
        String encodedName = URLEncoder.encode(name + " " + (dest.getCity() != null ? dest.getCity() : ""), StandardCharsets.UTF_8);
        
        return FoodRecommendation.builder()
                .destination(dest)
                .name(name)
                .description(desc)
                .cuisine(cuisine)
                .rating(rating)
                .priceRange(price)
                .address(address)
                .source(FoodSource.AI_RECOMMENDED)
                .zomatoUrl("https://www.zomato.com/search?q=" + encodedName)
                .swiggyUrl("https://www.swiggy.com/search?res=" + encodedName)
                .imageUrl(wikipediaImageClient.fetchImageForQuery(cuisine + " food"))
                .build();
    }
}
