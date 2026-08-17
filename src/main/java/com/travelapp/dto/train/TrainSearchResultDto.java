package com.travelapp.dto.train;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainSearchResultDto {
    private String trainNo;
    private String trainName;
    private String fromStnCode;
    private String toStnCode;
    private String fromTime;
    private String toTime;
    private String travelTime;
    private String distance;
    private Integer halts;
}
