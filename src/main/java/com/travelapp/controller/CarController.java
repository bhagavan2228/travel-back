package com.travelapp.controller;

import com.travelapp.dto.ApiResponse;
import com.travelapp.dto.car.CarSearchResponse;
import com.travelapp.service.CarService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cars")
@RequiredArgsConstructor
public class CarController {

    private final CarService carService;

    @GetMapping("/search")
    public ApiResponse<CarSearchResponse> search(
            @RequestParam(value = "location", defaultValue = "LHR") String location,
            @RequestParam(value = "pickupDate", defaultValue = "2026-09-01") String pickupDate,
            @RequestParam(value = "dropoffDate", defaultValue = "2026-09-05") String dropoffDate
    ) {
        return ApiResponse.ok(carService.searchCars(location, pickupDate, dropoffDate));
    }
}
