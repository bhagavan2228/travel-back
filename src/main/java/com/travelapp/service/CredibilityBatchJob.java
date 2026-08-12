package com.travelapp.service;

import com.travelapp.entity.CredibilityScore;
import com.travelapp.repository.CredibilityScoreRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CredibilityBatchJob {

    private final CredibilityScoreRepository credibilityScoreRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Scheduled(cron = "0 0 0 * * *") // Nightly at midnight
    @Transactional
    public void runNightlyBatchScoring() {
        log.info("Starting nightly credibility batch scoring...");
        List<CredibilityScore> allScores = credibilityScoreRepository.findAll();
        
        for (CredibilityScore score : allScores) {
            Long userId = score.getUser().getId();
            
            // Recompute from real DB counts
            Long tripsCount = entityManager.createQuery("SELECT COUNT(t) FROM Trip t WHERE t.user.id = :uid", Long.class)
                .setParameter("uid", userId)
                .getSingleResult();
                
            Long reviewsCount = entityManager.createQuery("SELECT COUNT(r) FROM Review r WHERE r.user.id = :uid", Long.class)
                .setParameter("uid", userId)
                .getSingleResult();
                
            Long reportsCount = entityManager.createQuery(
                "SELECT COUNT(rep) FROM Report rep " +
                "LEFT JOIN rep.review rev LEFT JOIN rep.comment c " +
                "WHERE (rev IS NOT NULL AND rev.user.id = :uid) " +
                "OR (c IS NOT NULL AND c.user.id = :uid)", Long.class)
                .setParameter("uid", userId)
                .getSingleResult();
            
            // Formula: base 50 + (trips * 10) + (reviews * 5) - (reports * 20)
            int newScore = 50 + (tripsCount.intValue() * 10) + (reviewsCount.intValue() * 5) - (reportsCount.intValue() * 20);
            newScore = Math.max(0, Math.min(100, newScore));
            
            score.setScore(newScore);
        }
        
        credibilityScoreRepository.saveAll(allScores);
        log.info("Nightly credibility batch scoring completed for {} users.", allScores.size());
    }
}
