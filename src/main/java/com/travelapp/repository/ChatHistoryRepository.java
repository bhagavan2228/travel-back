package com.travelapp.repository;

import com.travelapp.entity.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {
    List<ChatHistory> findByUserIdAndSessionIdOrderByTimestampAsc(Long userId, String sessionId);
}
