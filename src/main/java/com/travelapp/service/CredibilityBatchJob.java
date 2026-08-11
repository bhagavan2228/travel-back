package com.travelapp.service;

import com.travelapp.entity.CredibilityScore;
import com.travelapp.repository.CredibilityScoreRepository;
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

    @Scheduled(cron = "0 0 0 * * *") // Nightly at midnight
    @Transactional
    public void runNightlyBatchScoring() {
        log.info("Starting nightly credibility batch scoring...");
        List<CredibilityScore> allScores = credibilityScoreRepository.findAll();
        
        for (CredibilityScore score : allScores) {
            // Decay score slightly if no recent activity
            int currentScore = score.getScore();
            
            if (currentScore > 50) {
                currentScore -= 1;
            }
            
            currentScore = Math.min(100, currentScore);
            score.setScore(currentScore);
        }
        
        credibilityScoreRepository.saveAll(allScores);
        log.info("Nightly credibility batch scoring completed for {} users.", allScores.size());
    }
}
