package com.travelapp.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {
    private Long id;
    private Long destinationId;
    private String name;
    private String title; // kept for backward compatibility with my new service
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate eventDate; // kept for my service
    private String date;
    private String location;
    private String category;
    private String description;
    private Double expectedCrowd;
    private Boolean mockData;
}
