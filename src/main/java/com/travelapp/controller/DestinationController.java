package com.travelapp.controller;

import com.travelapp.dto.ApiResponse;
import com.travelapp.dto.destination.DestinationResponse;
import com.travelapp.dto.destination.HotelResponse;
import com.travelapp.dto.event.EventResponse;
import com.travelapp.dto.food.FoodResponse;
import com.travelapp.dto.review.ReviewRequest;
import com.travelapp.dto.review.ReviewResponse;
import com.travelapp.entity.User;
import com.travelapp.service.DestinationService;
import com.travelapp.service.EventPredictionService;
import com.travelapp.service.FoodService;
import com.travelapp.service.ReviewService;
import com.travelapp.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/destinations")
@RequiredArgsConstructor
public class DestinationController {

    private final DestinationService destinationService;
    private final FoodService foodService;
    private final EventPredictionService EventPredictionService;
    private final ReviewService reviewService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ApiResponse<List<DestinationResponse>> getAll() {
        return ApiResponse.ok(destinationService.findAll());
    }

    @GetMapping("/search")
    public ApiResponse<List<DestinationResponse>> search(@RequestParam("q") String query) {
        return ApiResponse.ok(destinationService.search(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<DestinationResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(destinationService.findById(id));
    }

    @GetMapping("/{id}/food")
    public ApiResponse<List<FoodResponse>> getFood(@PathVariable Long id) {
        return ApiResponse.ok(foodService.getByDestination(id));
    }

    @GetMapping("/{id}/events")
    public ApiResponse<List<EventResponse>> getEvents(@PathVariable Long id) {
        return ApiResponse.ok(EventPredictionService.getPredictedEventsForDestination(id));
    }

    @GetMapping("/{id}/reviews")
    public ApiResponse<List<ReviewResponse>> getReviews(@PathVariable Long id) {
        return ApiResponse.ok(reviewService.findByDestination(id));
    }

    @GetMapping("/{id}/hotels")
    public ApiResponse<List<HotelResponse>> getHotels(@PathVariable Long id) {
        return ApiResponse.ok(destinationService.getHotels(id));
    }

    @PostMapping("/{id}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewResponse> createReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request) {
        User user = securityUtils.getCurrentUser();
        return ApiResponse.ok(reviewService.create(id, request, user));
    }
}
