package com.travelapp.controller;

import com.travelapp.dto.ApiResponse;
import com.travelapp.dto.food.FoodResponse;
import com.travelapp.service.FoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/food")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;

    @GetMapping("/destination/{destinationId}")
    public ApiResponse<List<FoodResponse>> getByDestination(@PathVariable Long destinationId) {
        return ApiResponse.ok(foodService.getByDestination(destinationId));
    }
}
