package com.travelapp.service;

import com.travelapp.dto.food.FoodResponse;
import com.travelapp.entity.Destination;
import com.travelapp.entity.FoodRecommendation;
import com.travelapp.enums.FoodSource;
import com.travelapp.mapper.EntityMapper;
import com.travelapp.repository.FoodRecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FoodService {

    private final FoodRecommendationRepository foodRepository;
    private final DestinationService destinationService;

    @Transactional(readOnly = true)
    public List<FoodResponse> getByDestination(Long destinationId) {
        destinationService.getDestination(destinationId);
        List<FoodRecommendation> foods = foodRepository.findByDestinationId(destinationId);
        if (foods.isEmpty()) {
            return getMockFood(destinationId);
        }
        return foods.stream().map(EntityMapper::toFoodResponse).toList();
    }

    private List<FoodResponse> getMockFood(Long destinationId) {
        Destination dest = destinationService.getDestination(destinationId);
        String city = dest.getCity() != null ? dest.getCity() : dest.getName();
        
        String r1Name, r1Cuisine, r1Desc;
        String r2Name, r2Cuisine, r2Desc;
        String r3Name, r3Cuisine, r3Desc;

        if (city.toLowerCase().contains("warangal")) {
            r1Name = "Kakatiya Biryani & Kebabs"; r1Cuisine = "Telangana Traditional"; r1Desc = "Spicy country chicken curry and signature local biryani.";
            r2Name = "Bhadrakali Tiffins"; r2Cuisine = "South Indian"; r2Desc = "Crispy dosas and steaming hot idlis near the historic temple.";
            r3Name = "Telangana Spice Kitchen"; r3Cuisine = "Regional Indian"; r3Desc = "Rich local curries and traditional Telangana cuisine.";
        } else if (city.toLowerCase().contains("araku")) {
            r1Name = "Araku Coffee House"; r1Cuisine = "Cafe / Organic Coffee"; r1Desc = "Freshly brewed local organic coffee with baked snacks.";
            r2Name = "Tribal Spice Restaurant"; r2Cuisine = "Tribal Cuisine"; r2Desc = "Famous bamboo chicken (Bongu La Kodi) cooked without oil.";
            r3Name = "Valley View Dhaba"; r3Cuisine = "Andhra Regional"; r3Desc = "Spicy regional Andhra curry plates with scenic valley views.";
        } else if (city.toLowerCase().contains("goa")) {
            r1Name = "Fisherman's Wharf"; r1Cuisine = "Goan Seafood"; r1Desc = "Traditional Goan fish curry, butter garlic prawns, and local desserts.";
            r2Name = "Britto's Beach Shack"; r2Cuisine = "Coastal Seafood"; r2Desc = "Beachside restaurant serving fresh crabs and traditional Goan pork vindaloo.";
            r3Name = "Martin's Corner"; r3Cuisine = "Traditional Goan"; r3Desc = "Iconic Goan corner offering delicious chicken xacuti and local feni.";
        } else if (city.toLowerCase().contains("mumbai")) {
            r1Name = "Leopold Cafe & Bar"; r1Cuisine = "Irani Cafe"; r1Desc = "Historic spot famous for Keema Pav, cold beers, and classic Bombay vibes.";
            r2Name = "Elco Pani Puri Center"; r2Cuisine = "Mumbai Street Food"; r2Desc = "Spicy water-filled puris, sev puris, and authentic chaat.";
            r3Name = "Trishna Seafood Restaurant"; r3Cuisine = "Coastal Indian"; r3Desc = "World famous butter pepper garlic crabs and coastal Konkani style fish.";
        } else if (city.toLowerCase().contains("delhi")) {
            r1Name = "Karim's Old Delhi"; r1Cuisine = "Mughlai"; r1Desc = "Legendary Mughlai korma and charcoal-grilled seekh kebabs.";
            r2Name = "Paranthe Wali Gali"; r2Cuisine = "North Indian"; r2Desc = "Deep-fried stuffed crisp paranthas serving historically since decades.";
            r3Name = "Indian Accent"; r3Cuisine = "Modern Indian"; r3Desc = "Award-winning restaurant showcasing inventive modern Indian fusion.";
        } else {
            r1Name = city + " Bistro & Cafe"; r1Cuisine = "Local Cafe"; r1Desc = "Popular cafe featuring local delicacies of " + city + ".";
            r2Name = city + " Street Food Corner"; r2Cuisine = "Street Food"; r2Desc = "Authentic street food bites and regional specialities.";
            r3Name = city + " Heritage Kitchen"; r3Cuisine = "Traditional"; r3Desc = "Fine dining restaurant serving traditional recipes of " + city + ".";
        }

        return List.of(
                FoodResponse.builder().destinationId(destinationId).name(r1Name)
                        .description(r1Desc).cuisine(r1Cuisine)
                        .rating(4.5).priceRange("$$").source(FoodSource.AI_RECOMMENDED).build(),
                FoodResponse.builder().destinationId(destinationId).name(r2Name)
                        .description(r2Desc).cuisine(r2Cuisine)
                        .rating(4.7).priceRange("$").source(FoodSource.LOCAL).build(),
                FoodResponse.builder().destinationId(destinationId).name(r3Name)
                        .description(r3Desc).cuisine(r3Cuisine)
                        .rating(4.8).priceRange("$$$").source(FoodSource.PARTNER).build()
        );
    }
}
