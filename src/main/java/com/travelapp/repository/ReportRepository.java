package com.travelapp.repository;

import com.travelapp.entity.Report;
import com.travelapp.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status);
    List<Report> findAllByOrderByCreatedAtDesc();
    long countByCommentId(Long commentId);
    List<Report> findByCommentId(Long commentId);
}
