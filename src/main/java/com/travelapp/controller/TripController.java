package com.travelapp.controller;

import com.travelapp.dto.ApiResponse;
import com.travelapp.dto.trip.TripRequest;
import com.travelapp.dto.trip.TripResponse;
import com.travelapp.entity.User;
import com.travelapp.service.TripService;
import com.travelapp.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ApiResponse<List<TripResponse>> getAll() {
        User user = securityUtils.getCurrentUser();
        return ApiResponse.ok(tripService.findByUser(user));
    }

    @GetMapping("/{id}")
    public ApiResponse<TripResponse> getById(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser();
        return ApiResponse.ok(tripService.findById(id, user));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TripResponse> create(@Valid @RequestBody TripRequest request) {
        User user = securityUtils.getCurrentUser();
        return ApiResponse.ok(tripService.create(request, user));
    }

    @PutMapping("/{id}")
    public ApiResponse<TripResponse> update(@PathVariable Long id, @Valid @RequestBody TripRequest request) {
        User user = securityUtils.getCurrentUser();
        return ApiResponse.ok(tripService.update(id, request, user));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser();
        tripService.delete(id, user);
    }
}
