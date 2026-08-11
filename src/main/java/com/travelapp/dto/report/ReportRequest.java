package com.travelapp.dto.report;

import com.travelapp.enums.ReportReason;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportRequest {
    @NotNull
    private ReportReason reason;
    private String description;
}
