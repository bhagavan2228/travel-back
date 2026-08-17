package com.travelapp.repository;

import com.travelapp.entity.CarSearchCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface CarSearchCacheRepository extends JpaRepository<CarSearchCache, String> {
    void deleteByCreatedAtBefore(LocalDateTime expiryTime);
}
