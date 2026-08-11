package com.travelapp.dto.toxicity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToxicityResponse {
    private boolean toxic;
    private double score;
    private List<String> flaggedCategories;
    private String message;
}
