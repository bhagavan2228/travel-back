package com.travelapp.service;

import com.travelapp.dto.comment.CommentRequest;
import com.travelapp.dto.comment.CommentResponse;
import com.travelapp.dto.report.ReportRequest;
import com.travelapp.dto.report.ReportResponse;
import com.travelapp.entity.Comment;
import com.travelapp.entity.Report;
import com.travelapp.entity.Review;
import com.travelapp.entity.User;
import com.travelapp.enums.ReportStatus;
import com.travelapp.exception.ApiException;
import com.travelapp.mapper.EntityMapper;
import com.travelapp.repository.CommentRepository;
import com.travelapp.repository.ReportRepository;
import com.travelapp.service.ToxicityFilterService.ToxicityResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final ReviewService reviewService;
    private final ReportRepository reportRepository;
    private final ToxicityFilterService toxicityFilterService;

    @Transactional(readOnly = true)
    public List<CommentResponse> findByReview(Long reviewId) {
        reviewService.getReview(reviewId);
        return commentRepository.findByReviewIdOrderByCreatedAtAsc(reviewId).stream()
                .map(EntityMapper::toCommentResponse)
                .toList();
    }

    @Transactional
    public CommentResponse create(Long reviewId, CommentRequest request, User user) {
        ToxicityResult toxicity = toxicityFilterService.check(request.getContent());
        if (toxicity.score() > 0.75) {
            throw ApiException.badRequest("Comment flagged as highly inappropriate");
        }

        Review review = reviewService.getReview(reviewId);
        Comment parentComment = null;
        if (request.getParentCommentId() != null) {
            parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> ApiException.notFound("Parent comment not found"));
        }

        Comment comment = Comment.builder()
                .review(review)
                .user(user)
                .content(request.getContent())
                .likes(0)
                .isBlocked(toxicity.score() >= 0.5)
                .parentComment(parentComment)
                .build();
        return EntityMapper.toCommentResponse(commentRepository.save(comment));
    }

    @Transactional
    public CommentResponse like(Long commentId, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> ApiException.notFound("Comment not found"));
        comment.setLikes(comment.getLikes() + 1);
        return EntityMapper.toCommentResponse(commentRepository.save(comment));
    }

    @Transactional
    public ReportResponse reportComment(Long commentId, ReportRequest request, User reporter) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> ApiException.notFound("Comment not found"));

        Report report = Report.builder()
                .reporter(reporter)
                .comment(comment)
                .reason(request.getReason())
                .description(request.getDescription())
                .status(ReportStatus.PENDING)
                .build();
        
        report = reportRepository.save(report);

        long reportCount = reportRepository.countByCommentId(commentId);
        if (reportCount >= 3) {
            comment.setBlocked(true);
            commentRepository.save(comment);

            List<Report> reports = reportRepository.findByCommentId(commentId);
            reports.forEach(r -> r.setStatus(ReportStatus.ESCALATED));
            reportRepository.saveAll(reports);

            report.setStatus(ReportStatus.ESCALATED);
        }

        return EntityMapper.toReportResponse(report);
    }
}
