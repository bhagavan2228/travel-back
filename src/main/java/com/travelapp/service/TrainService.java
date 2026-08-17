package com.travelapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelapp.dto.train.TrainSearchResponse;
import com.travelapp.dto.train.TrainSearchResultDto;
import com.travelapp.entity.TrainSearchCache;
import com.travelapp.repository.TrainSearchCacheRepository;
import com.travelapp.service.integration.RailKitApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainService {

    private final RailKitApiClient railKitApiClient;
    private final TrainSearchCacheRepository trainCacheRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public TrainSearchResponse searchTrains(String from, String to, String date) {
        String cacheKey = String.format("train|%s|%s|%s", from.toUpperCase(), to.toUpperCase(), date);

        // 1. Check cache (1-hour TTL)
        trainCacheRepository.deleteByCreatedAtBefore(LocalDateTime.now().minusHours(1));
        Optional<TrainSearchCache> cached = trainCacheRepository.findById(cacheKey);

        if (cached.isPresent()) {
            try {
                TrainSearchResponse response = objectMapper.readValue(cached.get().getJsonResponse(), TrainSearchResponse.class);
                response.setCached(true);
                return response;
            } catch (Exception e) {
                log.warn("Failed to parse cached train data: {}", e.getMessage());
            }
        }

        // 2. Call RailKit API Proxy
        if (!railKitApiClient.isConfigured()) {
            return getFallbackTrains(from, to, date);
        }

        String jsonResponse = railKitApiClient.searchTrains(from, to, date);
        if (jsonResponse == null || jsonResponse.isEmpty()) {
            return getFallbackTrains(from, to, date);
        }

        // 3. Parse and Map
        List<TrainSearchResultDto> trains = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            if (root.has("data") && root.get("data").isArray()) {
                for (JsonNode node : root.get("data")) {
                    trains.add(TrainSearchResultDto.builder()
                            .trainNo(node.path("train_no").asText())
                            .trainName(node.path("train_name").asText())
                            .fromStnCode(node.path("from_stn_code").asText())
                            .toStnCode(node.path("to_stn_code").asText())
                            .fromTime(node.path("from_time").asText())
                            .toTime(node.path("to_time").asText())
                            .travelTime(node.path("travel_time").asText())
                            .distance(node.path("distance").asText())
                            .halts(node.path("halts").asInt(0))
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse train API response", e);
            return getFallbackTrains(from, to, date);
        }

        TrainSearchResponse response = TrainSearchResponse.builder()
                .origin(from)
                .destination(to)
                .date(date)
                .trains(trains)
                .isCached(false)
                .source("RAILKIT")
                .build();

        // 4. Save to cache
        try {
            TrainSearchCache cacheEntity = TrainSearchCache.builder()
                    .searchKey(cacheKey)
                    .jsonResponse(objectMapper.writeValueAsString(response))
                    .createdAt(LocalDateTime.now())
                    .build();
            trainCacheRepository.save(cacheEntity);
        } catch (Exception e) {
            log.warn("Could not cache train results", e);
        }

        return response;
    }

    private TrainSearchResponse getFallbackTrains(String from, String to, String date) {
        log.info("Generating fallback trains for {} -> {}", from, to);
        List<TrainSearchResultDto> trains = List.of(
                TrainSearchResultDto.builder()
                        .trainNo("12301")
                        .trainName("RAJDHANI EXP")
                        .fromStnCode(from)
                        .toStnCode(to)
                        .fromTime("16:50")
                        .toTime("09:55")
                        .travelTime("17:05 hrs")
                        .distance("1400")
                        .halts(5)
                        .build(),
                TrainSearchResultDto.builder()
                        .trainNo("12951")
                        .trainName("MUMBAI RAJDHANI")
                        .fromStnCode(from)
                        .toStnCode(to)
                        .fromTime("17:00")
                        .toTime("08:35")
                        .travelTime("15:35 hrs")
                        .distance("1384")
                        .halts(6)
                        .build()
        );

        return TrainSearchResponse.builder()
                .origin(from)
                .destination(to)
                .date(date)
                .trains(trains)
                .isCached(false)
                .source("FALLBACK")
                .build();
    }
}
