package com.travelapp.repository;

import com.travelapp.entity.HotelSearchCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface HotelSearchCacheRepository extends JpaRepository<HotelSearchCache, Long> {
    Optional<HotelSearchCache> findByCacheKey(String cacheKey);
    void deleteByCreatedAtBefore(LocalDateTime threshold);
}
