package com.travelapp.repository;

import com.travelapp.entity.TrainSearchCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface TrainSearchCacheRepository extends JpaRepository<TrainSearchCache, String> {
    void deleteByCreatedAtBefore(LocalDateTime expiryTime);
}
