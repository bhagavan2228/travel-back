package com.travelapp.dto.destination;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DestinationResponse {
    private Long id;
    private String name;
    private String country;
    private String city;
    private String state;
    private String description;
    private String imageUrl;
    private Double latitude;
    private Double longitude;
    private String climate;
    private String bestSeason;
    private String tags;
    private Integer exploredCount;
}
