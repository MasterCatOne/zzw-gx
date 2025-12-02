package com.zzw.zzwgx.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 统计响应DTO
 */
@Data
public class StatisticsResponse {
    
    private List<ProcessTimeStat> processTimeStats;
    private List<AdvanceLengthStat> advanceLengthStats;
    private List<OvertimeStat> overtimeStats;
    
    @Data
    public static class ProcessTimeStat {
        private String projectName;
        private Double averageTime;
        private Double savedTime;
    }
    
    @Data
    public static class AdvanceLengthStat {
        private String projectName;
        private Integer cycleCount;
        private Double advanceLength;
    }
    
    @Data
    public static class OvertimeStat {
        private String projectName;
        private Double overtime;
        private Double savedTime;
    }
}

