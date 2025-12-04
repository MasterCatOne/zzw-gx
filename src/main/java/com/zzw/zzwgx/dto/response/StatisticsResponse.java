package com.zzw.zzwgx.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 统计响应DTO
 */
@Data
@Schema(description = "统计响应")
public class StatisticsResponse {
    
    @Schema(description = "工序时间统计列表")
    private List<ProcessTimeStat> processTimeStats;
    
    @Schema(description = "进尺统计列表")
    private List<AdvanceLengthStat> advanceLengthStats;
    
    @Schema(description = "超时统计列表")
    private List<OvertimeStat> overtimeStats;
    
    @Data
    @Schema(description = "工序时间统计")
    public static class ProcessTimeStat {
        @Schema(description = "工点名称", example = "2号线2标段上行出口")
        private String projectName;
        
        @Schema(description = "平均时间（小时）", example = "2.5")
        private Double averageTime;
        
        @Schema(description = "节省时间（小时）", example = "0.5")
        private Double savedTime;
    }
    
    @Data
    @Schema(description = "进尺统计")
    public static class AdvanceLengthStat {
        @Schema(description = "工点名称", example = "2号线2标段上行出口")
        private String projectName;
        
        @Schema(description = "循环次数", example = "10")
        private Integer cycleCount;
        
        @Schema(description = "总进尺（米）", example = "5.5")
        private Double advanceLength;
    }
    
    @Data
    @Schema(description = "超时统计")
    public static class OvertimeStat {
        @Schema(description = "工点名称", example = "2号线2标段上行出口")
        private String projectName;
        
        @Schema(description = "超时时间（小时）", example = "1.5")
        private Double overtime;
        
        @Schema(description = "节省时间（小时）", example = "0.5")
        private Double savedTime;
    }
}

