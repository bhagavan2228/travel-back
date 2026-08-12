package com.travelapp.service;

import com.travelapp.dto.booking.BookingRequest;
import com.travelapp.dto.booking.BookingResponse;
import com.travelapp.entity.Booking;
import com.travelapp.entity.Trip;
import com.travelapp.entity.User;
import com.travelapp.enums.BookingStatus;
import com.travelapp.enums.BookingType;
import com.travelapp.exception.ApiException;
import com.travelapp.mapper.EntityMapper;
import com.travelapp.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import com.travelapp.service.integration.AmadeusApiClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TripService tripService;
    private final AmadeusApiClient amadeusApiClient;

    @Transactional
    public BookingResponse create(BookingRequest request, User user) {
        Trip trip = tripService.getUserTrip(request.getTripId(), user);

        Booking booking = Booking.builder()
                .trip(trip)
                .type(request.getType())
                .status(BookingStatus.CONFIRMED)
                .provider(request.getProvider() != null ? request.getProvider() : (request.getType() == BookingType.FLIGHT ? "Amadeus" : "TravelApp Demo"))
                .confirmationCode(request.getType() == BookingType.FLIGHT ? amadeusApiClient.generateBookingConfirmation() : "TA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase() + " (DEMO)")
                .price(request.getPrice())
                .details(request.getDetails())
                .build();

        return EntityMapper.toBookingResponse(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> findByUser(User user) {
        return bookingRepository.findByTripUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(EntityMapper::toBookingResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> findByTripId(Long tripId, User user) {
        tripService.getUserTrip(tripId, user);
        return bookingRepository.findByTripIdOrderByCreatedAtDesc(tripId).stream()
                .map(EntityMapper::toBookingResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse findById(Long id, User user) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Booking not found"));
        if (!booking.getTrip().getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("Access denied");
        }
        return EntityMapper.toBookingResponse(booking);
    }
}
