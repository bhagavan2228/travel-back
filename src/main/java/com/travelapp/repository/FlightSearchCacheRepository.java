package com.travelapp.repository;

import com.travelapp.entity.FlightSearchCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FlightSearchCacheRepository extends JpaRepository<FlightSearchCache, Long> {
    Optional<FlightSearchCache> findByCacheKey(String cacheKey);
}
