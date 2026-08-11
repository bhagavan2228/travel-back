package com.travelapp.dto.toxicity;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ToxicityRequest {
    @NotBlank
    private String text;
}
