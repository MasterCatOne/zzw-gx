package com.zzw.zzwgx.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 月度统计响应DTO
 */
@Data
@Schema(description = "月度统计响应")
public class MonthlyStatisticsResponse {
    
    @Schema(description = "月份（格式：2025-06）", example = "2025-06")
    private String month;
    
    @Schema(description = "工点统计数据列表")
    private List<ProjectStatistics> projectStatistics;
    
    @Data
    @Schema(description = "工点统计数据")
    public static class ProjectStatistics {
        
        @Schema(description = "工点ID", example = "1")
        private Long projectId;
        
        @Schema(description = "工点名称", example = "上行隧道入口工点")
        private String projectName;
        
        @Schema(description = "工序总时长（小时）", example = "120.5")
        private Double totalProcessTime;
        
        @Schema(description = "超耗时间（小时）", example = "15.3")
        private Double overtime;
        
        @Schema(description = "进尺长度（米）", example = "12.5")
        private BigDecimal advanceLength;
        
        @Schema(description = "超耗详情列表")
        private List<OvertimeDetail> overtimeDetails;
    }
    
    @Data
    @Schema(description = "超耗详情")
    public static class OvertimeDetail {
        
        @Schema(description = "工序ID", example = "1")
        private Long processId;
        
        @Schema(description = "工序名称", example = "扒渣")
        private String processName;
        
        @Schema(description = "循环号", example = "3")
        private Integer cycleNumber;
        
        @Schema(description = "控制时间（分钟）", example = "120")
        private Integer controlTime;
        
        @Schema(description = "实际时间（分钟）", example = "150")
        private Integer actualTime;
        
        @Schema(description = "超耗时间（分钟）", example = "30")
        private Integer overtimeMinutes;
        
        @Schema(description = "超耗原因", example = "设备故障导致延误")
        private String overtimeReason;
        
        @Schema(description = "实际开始时间", example = "2025-06-01T08:00:00")
        private LocalDateTime actualStartTime;
        
        @Schema(description = "实际结束时间", example = "2025-06-01T10:30:00")
        private LocalDateTime actualEndTime;
        
        @Schema(description = "操作员姓名", example = "张三")
        private String operatorName;
    }
}

