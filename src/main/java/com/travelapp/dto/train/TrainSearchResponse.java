package com.travelapp.dto.train;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainSearchResponse {
    private String origin;
    private String destination;
    private String date;
    private List<TrainSearchResultDto> trains;
    private boolean isCached;
    private String source;
}
