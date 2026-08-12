package com.travelapp.service.integration;

import com.travelapp.entity.Destination;
import com.travelapp.entity.FoodRecommendation;
import com.travelapp.enums.FoodSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MockGooglePlacesFoodProvider implements ThirdPartyFoodProvider {

    private final WikipediaImageClient wikipediaImageClient;

    @Override
    public List<FoodRecommendation> fetchTopRestaurants(Destination dest) {
        String city = dest.getCity() != null ? dest.getCity() : dest.getName();
        
        String r1Name, r1Cuisine, r1Desc, p1;
        String r2Name, r2Cuisine, r2Desc, p2;
        String r3Name, r3Cuisine, r3Desc, p3;

        if (city.toLowerCase().contains("warangal")) {
            r1Name = "Kakatiya Biryani & Kebabs"; r1Cuisine = "Telangana Traditional"; r1Desc = "Spicy country chicken curry and signature local biryani."; p1 = "$$";
            r2Name = "Bhadrakali Tiffins"; r2Cuisine = "South Indian"; r2Desc = "Crispy dosas and steaming hot idlis near the historic temple."; p2 = "$";
            r3Name = "Telangana Spice Kitchen"; r3Cuisine = "Regional Indian"; r3Desc = "Rich local curries and traditional Telangana cuisine."; p3 = "$$";
        } else if (city.toLowerCase().contains("araku")) {
            r1Name = "Araku Coffee House"; r1Cuisine = "Cafe / Organic Coffee"; r1Desc = "Freshly brewed local organic coffee with baked snacks."; p1 = "$";
            r2Name = "Tribal Spice Restaurant"; r2Cuisine = "Tribal Cuisine"; r2Desc = "Famous bamboo chicken (Bongu La Kodi) cooked without oil."; p2 = "$$";
            r3Name = "Valley View Dhaba"; r3Cuisine = "Andhra Regional"; r3Desc = "Spicy regional Andhra curry plates with scenic valley views."; p3 = "$";
        } else if (city.toLowerCase().contains("goa")) {
            r1Name = "Fisherman's Wharf"; r1Cuisine = "Goan Seafood"; r1Desc = "Traditional Goan fish curry, butter garlic prawns, and local desserts."; p1 = "$$$";
            r2Name = "Britto's Beach Shack"; r2Cuisine = "Coastal Seafood"; r2Desc = "Beachside restaurant serving fresh crabs and traditional Goan pork vindaloo."; p2 = "$$";
            r3Name = "Martin's Corner"; r3Cuisine = "Traditional Goan"; r3Desc = "Iconic Goan corner offering delicious chicken xacuti and local feni."; p3 = "$$";
        } else if (city.toLowerCase().contains("mumbai")) {
            r1Name = "Leopold Cafe & Bar"; r1Cuisine = "Irani Cafe"; r1Desc = "Historic spot famous for Keema Pav, cold beers, and classic Bombay vibes."; p1 = "$$";
            r2Name = "Elco Pani Puri Center"; r2Cuisine = "Mumbai Street Food"; r2Desc = "Spicy water-filled puris, sev puris, and authentic chaat."; p2 = "$";
            r3Name = "Trishna Seafood Restaurant"; r3Cuisine = "Coastal Indian"; r3Desc = "World famous butter pepper garlic crabs and coastal Konkani style fish."; p3 = "$$$";
        } else if (city.toLowerCase().contains("delhi")) {
            r1Name = "Karim's Old Delhi"; r1Cuisine = "Mughlai"; r1Desc = "Legendary Mughlai korma and charcoal-grilled seekh kebabs."; p1 = "$$";
            r2Name = "Paranthe Wali Gali"; r2Cuisine = "North Indian"; r2Desc = "Deep-fried stuffed crisp paranthas serving historically since decades."; p2 = "$";
            r3Name = "Indian Accent"; r3Cuisine = "Modern Indian"; r3Desc = "Award-winning restaurant showcasing inventive modern Indian fusion."; p3 = "$$$";
        } else {
            r1Name = city + " Bistro & Cafe"; r1Cuisine = "Local Cafe"; r1Desc = "Popular cafe featuring local delicacies of " + city + "."; p1 = "$$";
            r2Name = city + " Street Food Corner"; r2Cuisine = "Street Food"; r2Desc = "Authentic street food bites and regional specialities."; p2 = "$";
            r3Name = city + " Heritage Kitchen"; r3Cuisine = "Traditional"; r3Desc = "Fine dining restaurant serving traditional recipes of " + city + "."; p3 = "$$$";
        }

        return List.of(
                createRecommendation(dest, r1Name, r1Desc, r1Cuisine, 4.5, p1),
                createRecommendation(dest, r2Name, r2Desc, r2Cuisine, 4.7, p2),
                createRecommendation(dest, r3Name, r3Desc, r3Cuisine, 4.8, p3)
        );
    }

    private FoodRecommendation createRecommendation(Destination dest, String name, String desc, String cuisine, double rating, String price) {
        String encodedName = URLEncoder.encode(name + " " + dest.getCity(), StandardCharsets.UTF_8);
        
        return FoodRecommendation.builder()
                .destination(dest)
                .name(name)
                .description(desc)
                .cuisine(cuisine)
                .rating(rating)
                .priceRange(price)
                .address(dest.getCity() + ", " + dest.getCountry())
                .source(FoodSource.AI_RECOMMENDED)
                // Generate deep links for mobile apps or web search
                .zomatoUrl("https://www.zomato.com/search?q=" + encodedName)
                .swiggyUrl("https://www.swiggy.com/search?res=" + encodedName)
                .imageUrl(wikipediaImageClient.fetchImageForQuery(cuisine + " food"))
                .build();
    }
}
