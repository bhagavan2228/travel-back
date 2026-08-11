package com.travelapp.dto.credibility;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CredibilityResponse {
    private Long userId;
    private String userName;
    private Integer score;
    private Integer helpfulReviews;
    private Integer reportsResolved;
    private Integer totalReviews;
    private Integer rank;
}
