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
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ReportService reportService;

    @GetMapping("/reports")
    public ApiResponse<List<ReportResponse>> getReports() {
        return ApiResponse.ok(reportService.findAll());
    }
}
