package com.travelapp.controller;

import com.travelapp.dto.ApiResponse;
import com.travelapp.dto.report.ReportResponse;
import com.travelapp.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<ReportResponse>> getAll() {
        return ApiResponse.ok(reportService.findAll());
    }
}
