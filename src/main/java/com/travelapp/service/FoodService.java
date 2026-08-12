package com.travelapp.service;

import com.travelapp.dto.food.FoodResponse;
import com.travelapp.entity.Destination;
import com.travelapp.entity.FoodRecommendation;
import com.travelapp.mapper.EntityMapper;
import com.travelapp.repository.FoodRecommendationRepository;
import com.travelapp.service.integration.ThirdPartyFoodProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FoodService {

    private final FoodRecommendationRepository foodRepository;
    private final DestinationService destinationService;
    private final ThirdPartyFoodProvider foodProvider;
    private final com.travelapp.service.integration.MockGooglePlacesFoodProvider mockProvider;

    @Transactional
    public List<FoodResponse> getByDestination(Long destinationId) {
        Destination dest = destinationService.getDestination(destinationId);
        List<FoodRecommendation> foods = foodRepository.findByDestinationId(destinationId);
        
        if (foods.isEmpty()) {
            // Cache-aside: Fetch from external API and save to DB
            List<FoodRecommendation> apiData = foodProvider.fetchTopRestaurants(dest);
            if (apiData.isEmpty()) {
                apiData = mockProvider.fetchTopRestaurants(dest);
            }
            foods = foodRepository.saveAll(apiData);
        }
        
        return foods.stream().map(EntityMapper::toFoodResponse).toList();
    }
}
