package com.travelapp.service;

import com.travelapp.dto.report.ReportResponse;
import com.travelapp.mapper.EntityMapper;
import com.travelapp.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;

    @Transactional(readOnly = true)
    public List<ReportResponse> findAll() {
        return reportRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(EntityMapper::toReportResponse)
                .toList();
    }
}
