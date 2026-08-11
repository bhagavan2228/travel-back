package com.travelapp.service;

import com.travelapp.dto.credibility.CredibilityResponse;
import com.travelapp.entity.CredibilityScore;
import com.travelapp.mapper.EntityMapper;
import com.travelapp.repository.CredibilityScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CredibilityService {

    private final CredibilityScoreRepository credibilityScoreRepository;

    @Transactional(readOnly = true)
    public List<CredibilityResponse> getLeaderboard() {
        List<CredibilityScore> scores = credibilityScoreRepository.findTop10ByOrderByScoreDesc();
        List<CredibilityResponse> leaderboard = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) {
            leaderboard.add(EntityMapper.toCredibilityResponse(scores.get(i), i + 1));
        }
        return leaderboard;
    }
}
