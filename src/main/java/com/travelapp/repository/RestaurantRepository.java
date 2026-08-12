package com.travelapp.repository;

import com.travelapp.entity.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    Page<Restaurant> findByDestinationIdOrderByRankOrderAsc(Long destinationId, Pageable pageable);
    long countByDestinationId(Long destinationId);
    boolean existsByDestinationId(Long destinationId);
    java.util.Optional<Restaurant> findByGooglePlaceId(String googlePlaceId);
}
