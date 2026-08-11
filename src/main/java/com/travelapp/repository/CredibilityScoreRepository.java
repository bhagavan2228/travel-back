package com.travelapp.repository;

import com.travelapp.entity.CredibilityScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CredibilityScoreRepository extends JpaRepository<CredibilityScore, Long> {
    Optional<CredibilityScore> findByUserId(Long userId);
    List<CredibilityScore> findTop10ByOrderByScoreDesc();
}
