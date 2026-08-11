package com.travelapp.dto.route;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteStep {
    private String instruction;
    private Double distanceKm;
    private Integer durationMinutes;
}
