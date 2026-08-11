package com.travelapp.service;

import com.travelapp.dto.trip.TripRequest;
import com.travelapp.dto.trip.TripResponse;
import com.travelapp.entity.Destination;
import com.travelapp.entity.Trip;
import com.travelapp.entity.User;
import com.travelapp.enums.TripStatus;
import com.travelapp.exception.ApiException;
import com.travelapp.mapper.EntityMapper;
import com.travelapp.repository.BookingRepository;
import com.travelapp.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final DestinationService destinationService;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public List<TripResponse> findByUser(User user) {
        return tripRepository.findByUserIdOrderByStartDateDesc(user.getId()).stream()
                .map(EntityMapper::toTripResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TripResponse findById(Long id, User user) {
        Trip trip = getUserTrip(id, user);
        return EntityMapper.toTripResponse(trip);
    }

    @Transactional
    public TripResponse create(TripRequest request, User user) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw ApiException.badRequest("End date must be after start date");
        }
        Destination destination = destinationService.getDestination(request.getDestinationId());

        Trip trip = Trip.builder()
                .user(user)
                .title(request.getTitle())
                .destination(destination)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(request.getStatus() != null ? request.getStatus() : TripStatus.PLANNED)
                .notes(request.getNotes())
                .travelers(request.getTravelers())
                .build();

        return EntityMapper.toTripResponse(tripRepository.save(trip));
    }

    @Transactional
    public TripResponse update(Long id, TripRequest request, User user) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw ApiException.badRequest("End date must be after start date");
        }
        Trip trip = getUserTrip(id, user);
        Destination destination = destinationService.getDestination(request.getDestinationId());

        trip.setTitle(request.getTitle());
        trip.setDestination(destination);
        trip.setStartDate(request.getStartDate());
        trip.setEndDate(request.getEndDate());
        if (request.getStatus() != null) {
            trip.setStatus(request.getStatus());
        }
        trip.setNotes(request.getNotes());
        trip.setTravelers(request.getTravelers());

        return EntityMapper.toTripResponse(tripRepository.save(trip));
    }

    @Transactional
    public void delete(Long id, User user) {
        Trip trip = getUserTrip(id, user);
        bookingRepository.deleteAll(bookingRepository.findByTripIdOrderByCreatedAtDesc(trip.getId()));
        tripRepository.delete(trip);
    }

    public Trip getUserTrip(Long id, User user) {
        return tripRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> ApiException.notFound("Trip not found"));
    }
}
