package com.travelapp.repository;

import com.travelapp.entity.FoodRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodRecommendationRepository extends JpaRepository<FoodRecommendation, Long> {
    List<FoodRecommendation> findByDestinationId(Long destinationId);
}
