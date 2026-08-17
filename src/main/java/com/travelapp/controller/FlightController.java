package com.travelapp.controller;

import com.travelapp.dto.ApiResponse;
import com.travelapp.dto.flight.FlightSearchResponse;
import com.travelapp.service.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    @GetMapping("/search")
    public ApiResponse<FlightSearchResponse> search(
            @RequestParam(value = "origin", defaultValue = "LHR") String origin,
            @RequestParam(value = "destination", defaultValue = "JFK") String destination,
            @RequestParam(value = "date", required = false) String date,
            @RequestParam(value = "passengers", defaultValue = "1") Integer passengers,
            @RequestParam(value = "sortBy", defaultValue = "price") String sortBy) {

        FlightSearchResponse response = flightService.searchFlights(origin, destination, date, passengers, sortBy);
        return ApiResponse.ok(response);
    }
}
