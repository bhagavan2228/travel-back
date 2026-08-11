package com.travelapp.controller;

import com.travelapp.dto.ApiResponse;
import com.travelapp.dto.toxicity.ToxicityRequest;
import com.travelapp.dto.toxicity.ToxicityResponse;
import com.travelapp.service.ToxicityFilterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/toxicity")
@RequiredArgsConstructor
public class ToxicityController {

    private final ToxicityFilterService toxicityFilterService;

    @PostMapping("/check")
    public ApiResponse<ToxicityResponse> check(@Valid @RequestBody ToxicityRequest request) {
        return ApiResponse.ok(toxicityFilterService.check(request));
    }
}
