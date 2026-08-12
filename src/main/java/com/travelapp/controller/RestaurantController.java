package com.travelapp.controller;

import com.travelapp.dto.ApiResponse;
import com.travelapp.dto.PageResponse;

import com.travelapp.dto.food.RestaurantResponse;
import com.travelapp.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    @GetMapping("/destinations/{destinationId}/restaurants")
    public ApiResponse<PageResponse<RestaurantResponse>> getRestaurants(
            @PathVariable Long destinationId,
            @RequestParam(defaultValue = "0") int page) {
        return ApiResponse.ok(restaurantService.getRestaurants(destinationId, page));
    }

    @GetMapping("/restaurants/search")
    public ApiResponse<PageResponse<RestaurantResponse>> searchRestaurants(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "0") int page) {
        return ApiResponse.ok(restaurantService.searchRestaurantsByCoordinates(latitude, longitude, page));
    }
}
