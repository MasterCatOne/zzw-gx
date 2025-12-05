package com.zzw.zzwgx.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 每周超耗统计汇总响应DTO
 */
@Data
@Schema(description = "每周超耗统计汇总响应")
public class WeeklyOvertimeSummaryResponse {
    
    @Schema(description = "统计周期开始日期", example = "2025-12-01")
    private LocalDate weekStartDate;
    
    @Schema(description = "统计周期结束日期", example = "2025-12-07")
    private LocalDate weekEndDate;
    
    @Schema(description = "工点超耗统计列表（按超耗时间降序排列）")
    private List<ProjectOvertimeStat> projectOvertimeStats;
    
    @Schema(description = "工点节约时间排名列表（按节约时间降序排列）")
    private List<ProjectSavedTimeRank> savedTimeRanks;
    
    @Data
    @Schema(description = "工点超耗统计")
    public static class ProjectOvertimeStat {
        
        @Schema(description = "排名（按超耗时间）", example = "1")
        private Integer rank;
        
        @Schema(description = "工点ID", example = "1")
        private Long projectId;
        
        @Schema(description = "工点名称", example = "上行隧道入口工点")
        private String projectName;
        
        @Schema(description = "超耗时间（小时）", example = "15.5")
        private Double overtime;
        
        @Schema(description = "节约时间（小时）", example = "8.2")
        private Double savedTime;
        
        @Schema(description = "已完成工序数量", example = "25")
        private Integer completedProcessCount;
    }
    
    @Data
    @Schema(description = "工点节约时间排名")
    public static class ProjectSavedTimeRank {
        
        @Schema(description = "排名（按节约时间）", example = "1")
        private Integer rank;
        
        @Schema(description = "工点ID", example = "1")
        private Long projectId;
        
        @Schema(description = "工点名称", example = "上行隧道入口工点")
        private String projectName;
        
        @Schema(description = "节约时间（小时）", example = "8.2")
        private Double savedTime;
        
        @Schema(description = "超耗时间（小时）", example = "15.5")
        private Double overtime;
        
        @Schema(description = "已完成工序数量", example = "25")
        private Integer completedProcessCount;
    }
}

