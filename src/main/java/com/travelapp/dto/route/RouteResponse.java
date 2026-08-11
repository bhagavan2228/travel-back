package com.travelapp.dto.route;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponse {
    private Double distanceKm;
    private Integer durationMinutes;
    private String mode;
    private List<RouteStep> steps;
    private boolean mockData;
}
