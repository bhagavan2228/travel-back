package com.travelapp.service;

import com.travelapp.dto.notification.NotificationResponse;
import com.travelapp.entity.Notification;
import com.travelapp.entity.User;
import com.travelapp.enums.NotificationType;
import com.travelapp.exception.ApiException;
import com.travelapp.mapper.EntityMapper;
import com.travelapp.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<NotificationResponse> findByUser(User user) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        if (notifications.isEmpty()) {
            seedWelcomeNotification(user);
            notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        }
        return notifications.stream().map(EntityMapper::toNotificationResponse).toList();
    }

    @Transactional
    public NotificationResponse markAsRead(Long id, User user) {
        Notification notification = notificationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> ApiException.notFound("Notification not found"));
        notification.setRead(true);
        return EntityMapper.toNotificationResponse(notificationRepository.save(notification));
    }

    @Transactional
    public void seedWelcomeNotification(User user) {
        notificationRepository.save(Notification.builder()
                .user(user)
                .title("Welcome to AI Travel!")
                .message("Start planning your next adventure by exploring destinations.")
                .type(NotificationType.SYSTEM)
                .read(false)
                .build());
    }
}
