package com.travelapp.service.integration;

import com.travelapp.entity.Destination;
import com.travelapp.entity.FoodRecommendation;

import java.util.List;

public interface ThirdPartyFoodProvider {
    /**
     * Fetches top-rated restaurants from a third-party API for a given destination.
     *
     * @param destination The destination to fetch food for
     * @return A list of FoodRecommendation entities to be cached in the database
     */
    List<FoodRecommendation> fetchTopRestaurants(Destination destination);
}
