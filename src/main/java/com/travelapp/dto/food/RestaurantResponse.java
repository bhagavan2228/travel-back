package com.travelapp.dto.food;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantResponse {
    private Long id;
    private Long destinationId;
    private String name;
    private String googlePlaceId;
    private String cuisine;
    private Double rating;
    private Integer userRatingsTotal;
    private String address;
    private Double latitude;
    private Double longitude;
    private String priceLevel;
    private String website;
    private String googleMapsUri;
    private String businessStatus;
    private String imageUrl;
}
