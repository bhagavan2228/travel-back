package com.travelapp.controller;

import com.travelapp.dto.ApiResponse;
import com.travelapp.dto.notification.NotificationResponse;
import com.travelapp.entity.User;
import com.travelapp.service.NotificationService;
import com.travelapp.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ApiResponse<List<NotificationResponse>> getAll() {
        User user = securityUtils.getCurrentUser();
        return ApiResponse.ok(notificationService.findByUser(user));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markAsRead(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser();
        return ApiResponse.ok(notificationService.markAsRead(id, user));
    }
}
