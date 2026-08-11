package com.travelapp.mapper;

import com.travelapp.dto.booking.BookingResponse;
import com.travelapp.dto.comment.CommentResponse;
import com.travelapp.dto.credibility.CredibilityResponse;
import com.travelapp.dto.destination.DestinationResponse;
import com.travelapp.dto.food.FoodResponse;
import com.travelapp.dto.notification.NotificationResponse;
import com.travelapp.dto.report.ReportResponse;
import com.travelapp.dto.review.ReviewResponse;
import com.travelapp.dto.trip.TripResponse;
import com.travelapp.entity.*;

public final class EntityMapper {

    private EntityMapper() {}

    public static DestinationResponse toDestinationResponse(Destination d) {
        return DestinationResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .country(d.getCountry())
                .city(d.getCity())
                .state(d.getState())
                .description(d.getDescription())
                .imageUrl(d.getImageUrl())
                .latitude(d.getLatitude())
                .longitude(d.getLongitude())
                .climate(d.getClimate())
                .bestSeason(d.getBestSeason())
                .tags(d.getTags())
                .exploredCount(d.getExploredCount())
                .build();
    }

    public static TripResponse toTripResponse(Trip t) {
        return TripResponse.builder()
                .id(t.getId())
                .title(t.getTitle())
                .userId(t.getUser().getId())
                .destination(toDestinationResponse(t.getDestination()))
                .startDate(t.getStartDate())
                .endDate(t.getEndDate())
                .status(t.getStatus())
                .notes(t.getNotes())
                .travelers(t.getTravelers())
                .build();
    }

    public static BookingResponse toBookingResponse(Booking b) {
        return BookingResponse.builder()
                .id(b.getId())
                .tripId(b.getTrip().getId())
                .type(b.getType())
                .status(b.getStatus())
                .provider(b.getProvider())
                .confirmationCode(b.getConfirmationCode())
                .price(b.getPrice())
                .details(b.getDetails())
                .createdAt(b.getCreatedAt())
                .build();
    }

    public static FoodResponse toFoodResponse(FoodRecommendation f) {
        return FoodResponse.builder()
                .id(f.getId())
                .destinationId(f.getDestination().getId())
                .name(f.getName())
                .description(f.getDescription())
                .cuisine(f.getCuisine())
                .rating(f.getRating())
                .priceRange(f.getPriceRange())
                .address(f.getAddress())
                .source(f.getSource())
                .build();
    }

    public static NotificationResponse toNotificationResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }

    public static ReviewResponse toReviewResponse(Review r) {
        return toReviewResponse(r, null);
    }

    public static ReviewResponse toReviewResponse(Review r, Integer score) {
        return ReviewResponse.builder()
                .id(r.getId())
                .destinationId(r.getDestination().getId())
                .userId(r.getUser().getId())
                .userName(r.getUser().getFullName())
                .rating(r.getRating())
                .title(r.getTitle())
                .body(r.getBody())
                .isToxic(r.isToxic())
                .userCredibilityScore(score != null ? score : 100)
                .createdAt(r.getCreatedAt())
                .build();
    }

    public static CommentResponse toCommentResponse(Comment c) {
        return CommentResponse.builder()
                .id(c.getId())
                .reviewId(c.getReview().getId())
                .userId(c.getUser().getId())
                .userName(c.getUser().getFullName())
                .content(c.getContent())
                .likes(c.getLikes())
                .isBlocked(c.isBlocked())
                .parentCommentId(c.getParentComment() != null ? c.getParentComment().getId() : null)
                .createdAt(c.getCreatedAt())
                .build();
    }

    public static ReportResponse toReportResponse(Report r) {
        return ReportResponse.builder()
                .id(r.getId())
                .reporterId(r.getReporter().getId())
                .reporterName(r.getReporter().getFullName())
                .commentId(r.getComment() != null ? r.getComment().getId() : null)
                .reviewId(r.getReview() != null ? r.getReview().getId() : null)
                .reason(r.getReason())
                .description(r.getDescription())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .build();
    }

    public static CredibilityResponse toCredibilityResponse(CredibilityScore c, int rank) {
        return CredibilityResponse.builder()
                .userId(c.getUser().getId())
                .userName(c.getUser().getFullName())
                .score(c.getScore())
                .helpfulReviews(c.getHelpfulReviews())
                .reportsResolved(c.getReportsResolved())
                .totalReviews(c.getTotalReviews())
                .rank(rank)
                .build();
    }
}
