package com.travelapp.dto.food;

import com.travelapp.enums.FoodSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodResponse {
    private Long id;
    private Long destinationId;
    private String name;
    private String description;
    private String cuisine;
    private Double rating;
    private String priceRange;
    private String address;
    private FoodSource source;
}
