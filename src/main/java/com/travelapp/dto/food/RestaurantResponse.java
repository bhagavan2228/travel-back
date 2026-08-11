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
    private String cuisine;
    private Double rating;
    private Integer deliveryMinutes;
    private Integer costForTwo;
    private String imageUrl;
    private long menuItemCount;
}
