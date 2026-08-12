package com.travelapp.repository;

import com.travelapp.entity.PredictedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PredictedEventRepository extends JpaRepository<PredictedEvent, Long> {
    List<PredictedEvent> findByDestinationIdAndEventDateAfterOrderByEventDateAsc(Long destinationId, LocalDate date);
    List<PredictedEvent> findByDestinationIdOrderByEventDateAsc(Long destinationId);
}
