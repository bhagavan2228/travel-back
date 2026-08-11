package com.travelapp.repository;

import com.travelapp.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByTripUserIdOrderByCreatedAtDesc(Long userId);
    List<Booking> findByTripIdOrderByCreatedAtDesc(Long tripId);
}
