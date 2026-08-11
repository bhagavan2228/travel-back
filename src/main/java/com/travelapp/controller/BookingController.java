package com.travelapp.controller;

import com.travelapp.dto.ApiResponse;
import com.travelapp.dto.booking.BookingRequest;
import com.travelapp.dto.booking.BookingResponse;
import com.travelapp.entity.User;
import com.travelapp.service.BookingService;
import com.travelapp.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BookingResponse> create(@Valid @RequestBody BookingRequest request) {
        User user = securityUtils.getCurrentUser();
        return ApiResponse.ok(bookingService.create(request, user));
    }

    @GetMapping
    public ApiResponse<List<BookingResponse>> getAll() {
        User user = securityUtils.getCurrentUser();
        return ApiResponse.ok(bookingService.findByUser(user));
    }

    @GetMapping("/trip/{tripId}")
    public ApiResponse<List<BookingResponse>> getByTrip(@PathVariable Long tripId) {
        User user = securityUtils.getCurrentUser();
        return ApiResponse.ok(bookingService.findByTripId(tripId, user));
    }
}
