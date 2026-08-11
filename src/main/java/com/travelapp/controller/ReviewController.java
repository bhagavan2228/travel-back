package com.travelapp.controller;

import com.travelapp.dto.ApiResponse;
import com.travelapp.dto.comment.CommentRequest;
import com.travelapp.dto.comment.CommentResponse;
import com.travelapp.entity.User;
import com.travelapp.service.CommentService;
import com.travelapp.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final CommentService commentService;
    private final SecurityUtils securityUtils;

    @GetMapping("/{id}/comments")
    public ApiResponse<List<CommentResponse>> getComments(@PathVariable Long id) {
        return ApiResponse.ok(commentService.findByReview(id));
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentResponse> createComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentRequest request) {
        User user = securityUtils.getCurrentUser();
        return ApiResponse.ok(commentService.create(id, request, user));
    }
}
