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
        String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();

        chatHistoryRepository.save(ChatHistory.builder()
                .user(user)
                .sessionId(sessionId)
                .role(ChatRole.USER)
                .content(request.getMessage())
                .build());

        String context = buildContext(request);
        SseEmitter emitter = new SseEmitter(60000L); // 60 seconds timeout

        executor.execute(() -> {
            try {
                String apiKey = appProperties.getAssistant().getApiKey();
                StringBuilder fullResponse = new StringBuilder();

                if (apiKey != null && !apiKey.isBlank()) {
                    try {
                        streamFromGemini(request.getMessage(), context, apiKey, emitter, fullResponse);
                    } catch (Exception e) {
                        log.error("Gemini API streaming failed. Falling back to mock data.", e);
                        sendMockStream(request.getMessage(), context, emitter, fullResponse);
                    }
                } else {
                    sendMockStream(request.getMessage(), context, emitter, fullResponse);
                }

                // Save full response to DB after streaming is done
                chatHistoryRepository.save(ChatHistory.builder()
                        .user(user)
                        .sessionId(sessionId)
                        .role(ChatRole.ASSISTANT)
                        .content(fullResponse.toString())
                        .build());

                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();

            } catch (Exception e) {
                log.error("Error in chat streaming", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private String buildContext(ChatRequest request) {
        if (request.getDestinationId() != null) {
            return destinationRepository.findById(request.getDestinationId())
                    .map(Destination::getName)
                    .orElse("unknown destination");
        }
        return "general travel";
    }

    private void streamFromGemini(String message, String context, String apiKey, SseEmitter emitter, StringBuilder fullResponse) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:streamGenerateContent?key=" + apiKey;

        String systemPrompt = "You are a helpful travel assistant. Context: " + context + "\n\nUser: " + message;
        
        var body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", systemPrompt)))
                )
        );

        Flux<String> responseStream = webClientBuilder.build()
                .post()
                .uri(url)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class);

        responseStream.subscribe(chunk -> {
            try {
                // Parse Gemini chunk to get text
                JsonNode rootNode = objectMapper.readTree(chunk);
                if (rootNode.has("candidates")) {
                    JsonNode candidates = rootNode.get("candidates");
                    if (candidates.isArray() && candidates.size() > 0) {
                        JsonNode content = candidates.get(0).get("content");
                        if (content != null && content.has("parts")) {
                            JsonNode parts = content.get("parts");
                            if (parts.isArray() && parts.size() > 0) {
                                String text = parts.get(0).get("text").asText();
                                fullResponse.append(text);
                                emitter.send(SseEmitter.event().name("message").data(text));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error parsing Gemini stream chunk", e);
            }
        }, error -> {
            log.error("Gemini stream error", error);
        });
        
        // Block to keep executor thread alive until stream completes
        responseStream.blockLast(); 
    }

    private void sendMockStream(String message, String context, SseEmitter emitter, StringBuilder fullResponse) throws Exception {
        String mockReply = generateMockReply(message, context);
        String[] words = mockReply.split(" ");
        for (String word : words) {
            fullResponse.append(word).append(" ");
            emitter.send(SseEmitter.event().name("message").data(word + " "));
            Thread.sleep(100); // Simulate network delay
        }
    }

    private String generateMockReply(String message, String context) {
        String lower = message.toLowerCase();
        String ctxLower = context.toLowerCase();

        if (ctxLower.contains("warangal")) {
            return "When in Warangal, you must try the spicy Telangana traditional Biryani and explore the historical Thousand Pillar Temple!";
        }
        if (ctxLower.contains("goa")) {
            return "Goa is a paradise for seafood! You must try the Goan Fish Curry, and relax on the beaches of Anjuna.";
        }
        return "I'd love to help you plan your trip to " + context + "! You can ask me about attractions, food, or weather.";
    }
}
