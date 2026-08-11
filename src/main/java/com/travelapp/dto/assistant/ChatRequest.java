package com.travelapp.dto.assistant;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {
    @NotBlank
    private String message;
    private String sessionId;
    private Long destinationId;
    private Long tripId;
}
