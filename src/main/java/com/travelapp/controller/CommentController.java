package com.travelapp.controller;

import com.travelapp.dto.ApiResponse;
import com.travelapp.dto.comment.CommentResponse;
import com.travelapp.dto.report.ReportRequest;
import com.travelapp.dto.report.ReportResponse;
import com.travelapp.entity.User;
import com.travelapp.service.CommentService;
import com.travelapp.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final SecurityUtils securityUtils;

    @PostMapping("/{id}/like")
    public ApiResponse<CommentResponse> like(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser();
        return ApiResponse.ok(commentService.like(id, user));
    }

    @PostMapping("/{id}/report")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReportResponse> report(
            @PathVariable Long id,
            @Valid @RequestBody ReportRequest request) {
        User user = securityUtils.getCurrentUser();
        return ApiResponse.ok(commentService.reportComment(id, request, user));
    }
}
