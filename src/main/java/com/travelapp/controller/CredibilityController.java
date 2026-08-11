package com.travelapp.controller;

import com.travelapp.dto.ApiResponse;
import com.travelapp.dto.credibility.CredibilityResponse;
import com.travelapp.service.CredibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/credibility")
@RequiredArgsConstructor
public class CredibilityController {

    private final CredibilityService credibilityService;

    @GetMapping("/leaderboard")
    public ApiResponse<List<CredibilityResponse>> leaderboard() {
        return ApiResponse.ok(credibilityService.getLeaderboard());
    }
}
