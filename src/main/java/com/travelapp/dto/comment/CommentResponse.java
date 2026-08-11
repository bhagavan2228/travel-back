package com.travelapp.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private Long reviewId;
    private Long userId;
    private String userName;
    private String content;
    private Integer likes;
    private boolean isBlocked;
    private Long parentCommentId;
    private LocalDateTime createdAt;
}
