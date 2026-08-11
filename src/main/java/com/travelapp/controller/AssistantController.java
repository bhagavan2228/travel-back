package com.travelapp.controller;

import com.travelapp.dto.assistant.ChatRequest;
import com.travelapp.entity.User;
import com.travelapp.service.TravelAssistantService;
import com.travelapp.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final TravelAssistantService travelAssistantService;
    private final SecurityUtils securityUtils;

    @PostMapping(value = "/chat", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody ChatRequest request) {
        User user = securityUtils.getCurrentUser();
        return travelAssistantService.chatStream(request, user);
    }
}
