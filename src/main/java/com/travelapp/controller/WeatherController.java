package com.travelapp.controller;

import com.travelapp.dto.ApiResponse;
import com.travelapp.dto.weather.WeatherResponse;
import com.travelapp.entity.User;
import com.travelapp.service.WeatherService;
import com.travelapp.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;
    private final SecurityUtils securityUtils;

    @GetMapping("/destination/{destinationId}")
    public ApiResponse<WeatherResponse> getByDestination(@PathVariable Long destinationId) {
        return ApiResponse.ok(weatherService.getByDestination(destinationId));
    }

    @GetMapping("/trip/{tripId}")
    public ApiResponse<WeatherResponse> getByTrip(@PathVariable Long tripId) {
        User user = securityUtils.getCurrentUser();
        return ApiResponse.ok(weatherService.getByTrip(tripId, user));
    }
}
