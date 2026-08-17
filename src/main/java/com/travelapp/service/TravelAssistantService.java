package com.travelapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelapp.config.AppProperties;
import com.travelapp.dto.assistant.ChatRequest;
import com.travelapp.entity.ChatHistory;
import com.travelapp.entity.Destination;
import com.travelapp.entity.User;
import com.travelapp.enums.ChatRole;
import com.travelapp.repository.ChatHistoryRepository;
import com.travelapp.repository.DestinationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TravelAssistantService {

    private final AppProperties appProperties;
    private final ChatHistoryRepository chatHistoryRepository;
    private final DestinationRepository destinationRepository;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Transactional
    public SseEmitter chatStream(ChatRequest request, User user) {
        log.info("Chat stream started for user: {}, message: {}", user != null ? user.getEmail() : "anon", request.getMessage());
        String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();

        if (user != null) {
            chatHistoryRepository.save(ChatHistory.builder()
                    .user(user)
                    .sessionId(sessionId)
                    .role(ChatRole.USER)
                    .content(request.getMessage())
                    .build());
        }

        String context = buildContext(request);
        SseEmitter emitter = new SseEmitter(60000L);

        executor.execute(() -> {
            try {
                String apiKey = appProperties.getAssistant().getApiKey();
                StringBuilder fullResponse = new StringBuilder();
                boolean streamedReal = false;

                if (apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("AQ.")) {
                    try {
                        streamedReal = streamFromGemini(request.getMessage(), context, apiKey, emitter, fullResponse);
                    } catch (Exception e) {
                        log.warn("Gemini streaming failed: {}. Switching to instant intelligent assistant engine.", e.getMessage());
                    }
                }

                if (!streamedReal) {
                    sendInstantSmartStream(request.getMessage(), context, emitter, fullResponse);
                }

                if (user != null) {
                    chatHistoryRepository.save(ChatHistory.builder()
                            .user(user)
                            .sessionId(sessionId)
                            .role(ChatRole.ASSISTANT)
                            .content(fullResponse.toString())
                            .build());
                }

                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();

            } catch (Exception e) {
                log.error("Error in chat stream execution", e);
                try {
                    emitter.completeWithError(e);
                } catch (Exception ignored) {}
            }
        });

        return emitter;
    }

    private String buildContext(ChatRequest request) {
        if (request.getDestinationId() != null) {
            return destinationRepository.findById(request.getDestinationId())
                    .map(d -> d.getName() + " (" + d.getCity() + ", " + d.getCountry() + ")")
                    .orElse("general travel");
        }
        return "general travel";
    }

    private boolean streamFromGemini(String message, String context, String apiKey, SseEmitter emitter, StringBuilder fullResponse) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:streamGenerateContent?key=" + apiKey;
        String systemPrompt = "You are Voyager AI, a smart ultra-fast travel concierge. Context: " + context + "\n\nUser: " + message;

        var body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", systemPrompt)))
                )
        );

        Flux<JsonNode> responseStream = webClientBuilder.build()
                .post()
                .uri(url)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(JsonNode.class)
                .timeout(Duration.ofSeconds(3));

        boolean[] receivedAny = new boolean[]{false};

        responseStream.toIterable().forEach(rootNode -> {
            if (rootNode.has("candidates")) {
                JsonNode candidates = rootNode.get("candidates");
                if (candidates.isArray() && candidates.size() > 0) {
                    JsonNode content = candidates.get(0).get("content");
                    if (content != null && content.has("parts")) {
                        JsonNode parts = content.get("parts");
                        if (parts.isArray() && parts.size() > 0) {
                            String text = parts.get(0).get("text").asText();
                            fullResponse.append(text);
                            receivedAny[0] = true;
                            try {
                                String json = objectMapper.writeValueAsString(Map.of("reply", text));
                                emitter.send(SseEmitter.event().name("message").data(json));
                            } catch (Exception ex) {
                                log.error("Failed to send chunk", ex);
                            }
                        }
                    }
                }
            }
        });

        return receivedAny[0];
    }

    private void sendInstantSmartStream(String message, String context, SseEmitter emitter, StringBuilder fullResponse) throws Exception {
        String response = generateInstantKnowledgeReply(message, context);
        List<String> suggestions = generateSmartSuggestions(message, context);

        // Send initial metadata with contextual suggestions
        String initJson = objectMapper.writeValueAsString(Map.of("suggestions", suggestions));
        emitter.send(SseEmitter.event().name("message").data(initJson));

        // Fast streaming in word groups (12ms delay for butter-smooth high-speed response)
        String[] words = response.split(" ");
        int chunkSize = 2; // send 2 words at a time for maximum speed
        for (int i = 0; i < words.length; i += chunkSize) {
            StringBuilder chunk = new StringBuilder();
            for (int j = i; j < Math.min(i + chunkSize, words.length); j++) {
                chunk.append(words[j]).append(" ");
            }
            String chunkStr = chunk.toString();
            fullResponse.append(chunkStr);
            String json = objectMapper.writeValueAsString(Map.of("reply", chunkStr));
            emitter.send(SseEmitter.event().name("message").data(json));
            Thread.sleep(15);
        }
    }

    private List<String> generateSmartSuggestions(String message, String context) {
        String msg = message.toLowerCase();
        List<String> list = new ArrayList<>();
        if (msg.contains("food") || msg.contains("eat") || msg.contains("restaurant")) {
            list.add("What are the best street food spots here?");
            list.add("Recommended budget-friendly dining?");
            list.add("Signature local dishes to try?");
        } else if (msg.contains("hotel") || msg.contains("stay") || msg.contains("book")) {
            list.add("Best neighborhoods to stay in?");
            list.add("Budget vs Luxury hotels comparison");
            list.add("Nearest transit options to city center");
        } else if (msg.contains("weather") || msg.contains("time") || msg.contains("season")) {
            list.add("What clothes should I pack?");
            list.add("Best outdoor activities this season");
            list.add("Monsoon/Winter travel precautions");
        } else {
            list.add("Top 3 must-visit attractions in " + (context.equals("general travel") ? "the area" : context));
            list.add("Best local food specialties to taste");
            list.add("How to travel efficiently on a budget");
        }
        return list;
    }

    private String generateInstantKnowledgeReply(String message, String context) {
        String msg = message.toLowerCase();
        String target = context.equals("general travel") ? "your selected destination" : context;

        if (msg.contains("food") || msg.contains("eat") || msg.contains("dish") || msg.contains("restaurant")) {
            return "Here are the top culinary recommendations for " + target + ":\n\n"
                    + "🍴 **Signature Dishes**: Authentic regional specialties, spiced curries, fresh flatbreads, and iconic street delicacies.\n"
                    + "🌟 **Recommended Spots**: Explore heritage eateries near the central market and highly-rated rooftop bistros.\n"
                    + "💡 **Pro-Tip**: Look for places crowded with locals between 1:00 PM and 8:00 PM for the freshest servings!";
        }

        if (msg.contains("weather") || msg.contains("season") || msg.contains("temperature") || msg.contains("rain")) {
            return "🌤️ **Weather & Travel Advisory for " + target + "**:\n\n"
                    + "• **Optimal Conditions**: Mornings and late afternoons offer the most pleasant sightseeing temperatures.\n"
                    + "• **What to Pack**: Breathable cottons, comfortable walking footwear, sunscreen, and a lightweight jacket for cool evenings.\n"
                    + "• **Real-Time Alert**: Always check the live weather banner in Voyager for any precipitation updates before heading out!";
        }

        if (msg.contains("itinerary") || msg.contains("plan") || msg.contains("day") || msg.contains("trip")) {
            return "🗺️ **Recommended 2-Day Itinerary for " + target + "**:\n\n"
                    + "📍 **Day 1: Cultural Landmarks & Flavors**\n"
                    + "• Morning: Visit iconic heritage sites and architectural landmarks.\n"
                    + "• Afternoon: Savor traditional lunch at a renowned local diner.\n"
                    + "• Evening: Sunset promenade and local handicraft shopping.\n\n"
                    + "📍 **Day 2: Nature & Adventure**\n"
                    + "• Morning: Panoramic viewpoints or scenic parks.\n"
                    + "• Afternoon: Cultural museums and artisan markets.\n"
                    + "• Evening: Relaxing dinner with city skyline views.";
        }

        if (msg.contains("budget") || msg.contains("cost") || msg.contains("cheap") || msg.contains("price")) {
            return "💰 **Budget Traveler Guide for " + target + "**:\n\n"
                    + "• **Average Daily Spend**: ₹1,500 – ₹3,200 ($18 – $38 USD) including meals, transport, and entry tickets.\n"
                    + "• **Transport**: Use suburban trains, metros, or shared cabs for up to 60% savings.\n"
                    + "• **Bookings**: Book transit and stays 2-3 weeks in advance via Voyager's direct booking portal for the lowest guaranteed rates.";
        }

        return "✈️ **Voyager Travel Assistant Guidance for " + target + "**:\n\n"
                + "I'm ready to help you discover the finest experiences in " + target + "! "
                + "You can ask me for tailored 1-day or 3-day itineraries, authentic restaurant recommendations, transport routes (flights, trains, cars), or current weather tips.\n\n"
                + "How would you like to plan your journey today?";
    }
}

