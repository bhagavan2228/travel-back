package com.travelapp.service;

import com.travelapp.dto.review.ReviewRequest;
import com.travelapp.dto.review.ReviewResponse;
import com.travelapp.entity.Destination;
import com.travelapp.entity.Review;
import com.travelapp.entity.User;
import com.travelapp.exception.ApiException;
import com.travelapp.mapper.EntityMapper;
import com.travelapp.repository.CredibilityScoreRepository;
import com.travelapp.repository.ReviewRepository;
import com.travelapp.service.ToxicityFilterService.ToxicityResult;
import com.travelapp.entity.CredibilityScore;
import com.travelapp.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final DestinationService destinationService;
    private final CredibilityScoreRepository credibilityScoreRepository;
    private final ToxicityFilterService toxicityFilterService;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public List<ReviewResponse> findByDestination(Long destinationId) {
        destinationService.getDestination(destinationId);
        List<Review> reviews = reviewRepository.findByDestinationIdOrderByCreatedAtDesc(destinationId);

        Long currentUserId = null;
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            try {
                currentUserId = securityUtils.getCurrentUser().getId();
            } catch (Exception e) {
                // Ignore and keep null
            }
        }

        final Long finalUserId = currentUserId;

        return reviews.stream()
                .map(r -> {
                    int score = credibilityScoreRepository.findByUserId(r.getUser().getId())
                            .map(CredibilityScore::getScore)
                            .orElse(100);
                    return EntityMapper.toReviewResponse(r, score);
                })
                .sorted((r1, r2) -> {
                    if (finalUserId != null) {
                        if (r1.getUserId().equals(finalUserId) && !r2.getUserId().equals(finalUserId)) return -1;
                        if (!r1.getUserId().equals(finalUserId) && r2.getUserId().equals(finalUserId)) return 1;
                    }
                    return Integer.compare(r2.getUserCredibilityScore(), r1.getUserCredibilityScore());
                })
                .toList();
    }

    @Transactional
    public ReviewResponse create(Long destinationId, ReviewRequest request, User user) {
        ToxicityResult toxicity = toxicityFilterService.check(request.getTitle() + " " + request.getBody());
        if (toxicity.score() > 0.75) {
            throw ApiException.badRequest("Review content flagged as highly inappropriate: " + toxicity.message());
        }

        Destination destination = destinationService.getDestination(destinationId);
        Review review = Review.builder()
                .user(user)
                .destination(destination)
                .rating(request.getRating())
                .title(request.getTitle())
                .body(request.getBody())
                .isToxic(toxicity.score() >= 0.5)
                .build();
        review = reviewRepository.save(review);

        credibilityScoreRepository.findByUserId(user.getId()).ifPresent(score -> {
            score.setTotalReviews(score.getTotalReviews() + 1);
            score.setScore(Math.min(100, score.getScore() + 2));
            credibilityScoreRepository.save(score);
        });

        int userScore = credibilityScoreRepository.findByUserId(user.getId())
                .map(CredibilityScore::getScore)
                .orElse(100);

        return EntityMapper.toReviewResponse(review, userScore);
    }

    public Review getReview(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Review not found"));
    }
}
