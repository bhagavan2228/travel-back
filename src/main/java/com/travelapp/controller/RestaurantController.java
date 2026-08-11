package com.travelapp.controller;

import com.travelapp.dto.ApiResponse;
import com.travelapp.dto.PageResponse;
import com.travelapp.dto.food.MenuItemResponse;
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

    @GetMapping("/restaurants/{restaurantId}/menu")
    public ApiResponse<PageResponse<MenuItemResponse>> getMenu(
            @PathVariable Long restaurantId,
            @RequestParam(defaultValue = "0") int page) {
        return ApiResponse.ok(restaurantService.getMenuItems(restaurantId, page));
    }
}
