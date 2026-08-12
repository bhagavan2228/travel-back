package com.travelapp.service;

import com.travelapp.dto.event.EventResponse;
import com.travelapp.entity.Destination;
import com.travelapp.entity.PredictedEvent;
import com.travelapp.mapper.EntityMapper;
import com.travelapp.repository.PredictedEventRepository;
import com.travelapp.service.integration.GeminiApiClient;
import com.travelapp.service.integration.NewsApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPredictionService {

    private final PredictedEventRepository eventRepository;
    private final DestinationService destinationService;
    private final NewsApiClient newsApiClient;
    private final GeminiApiClient geminiApiClient;

    @Transactional
    public List<EventResponse> getPredictedEventsForDestination(Long destinationId) {
        Destination dest = destinationService.getDestination(destinationId);
        
        // 1. Check Database for recent future events
        List<PredictedEvent> existingEvents = eventRepository.findByDestinationIdAndEventDateAfterOrderByEventDateAsc(destinationId, LocalDate.now().minusDays(1));
        
        if (!existingEvents.isEmpty()) {
            log.info("Returning cached predicted events for destination {}", destinationId);
            return existingEvents.stream().map(EntityMapper::toEventResponse).toList();
        }

        log.info("No cached events found. Fetching from News & Gemini API for destination {}", destinationId);
        
        // 2. Fetch context from News API
        String city = dest.getCity() != null ? dest.getCity() : dest.getName();
        String newsContext = newsApiClient.fetchNewsForDestination(city);

        // 3. Process with Gemini API
        List<Map<String, String>> predictedEventsData = geminiApiClient.extractEventsFromNews(newsContext, city);

        // 4. Save to Database (Cache-aside)
        List<PredictedEvent> eventsToSave = predictedEventsData.stream().map(data -> {
            LocalDate eventDate;
            try {
                eventDate = LocalDate.parse(data.get("date"));
            } catch (Exception e) {
                eventDate = LocalDate.now().plusDays((long) (Math.random() * 10) + 1); // Random future date fallback
            }

            return PredictedEvent.builder()
                    .destination(dest)
                    .title(data.get("title"))
                    .eventDate(eventDate)
                    .category(data.get("category"))
                    .description(data.get("description"))
                    .build();
        }).toList();

        List<PredictedEvent> savedEvents = eventRepository.saveAll(eventsToSave);

        // 5. Return to controller
        return savedEvents.stream().map(EntityMapper::toEventResponse).toList();
    }
}
