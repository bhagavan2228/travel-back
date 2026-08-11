package com.travelapp.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Long destinationId;
    private Long userId;
    private String userName;
    private Integer rating;
    private String title;
    private String body;
    private Integer userCredibilityScore;
    private boolean isToxic;
    private LocalDateTime createdAt;
}
