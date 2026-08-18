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
public class TrainSearchResultDto {
    private String trainNo;
    private String trainName;
    private String trainType;
    private String fromStnCode;
    private String toStnCode;
    private String fromTime;
    private String toTime;
    private String travelTime;
    private String distance;
    private Integer halts;
    private Double price;
    private List<TrainClassInfo> classes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrainClassInfo {
        private String name;
        private Double price;
        private String vacancies;
    }
}
