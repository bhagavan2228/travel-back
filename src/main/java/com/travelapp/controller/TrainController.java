package com.travelapp.controller;

import com.travelapp.dto.ApiResponse;
import com.travelapp.dto.train.TrainSearchResponse;
import com.travelapp.service.TrainService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trains")
@RequiredArgsConstructor
public class TrainController {

    private final TrainService trainService;

    @GetMapping("/search")
    public ApiResponse<TrainSearchResponse> search(
            @RequestParam(value = "origin", defaultValue = "NDLS") String origin,
            @RequestParam(value = "destination", defaultValue = "MMCT") String destination,
            @RequestParam(value = "date", defaultValue = "15-04-2025") String date
    ) {
        return ApiResponse.ok(trainService.searchTrains(origin, destination, date));
    }
}
