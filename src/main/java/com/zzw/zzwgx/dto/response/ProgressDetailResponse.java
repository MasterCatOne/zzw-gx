package com.zzw.zzwgx.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 进度详情响应DTO
 */
@Data
@Schema(description = "进度详情响应")
public class ProgressDetailResponse {
    
    @Schema(description = "循环ID", example = "1")
    private Long cycleId;
    
    @Schema(description = "循环次数", example = "3")
    private Integer cycleNumber;
    
    @Schema(description = "循环状态", example = "IN_PROGRESS")
    private String cycleStatus;
    
    @Schema(description = "循环状态描述（中文）", example = "进行中")
    private String cycleStatusDesc;
    
    @Schema(description = "控制时长标准（分钟）", example = "360")
    private Integer controlDuration;
    
    @Schema(description = "循环进尺（米）", example = "0.5")
    private BigDecimal advanceLength;
    
    @Schema(description = "当前工序", example = "放样测量")
    private String currentProcess;
    
    @Schema(description = "上循环实际结束时间", example = "2025-11-04T08:00:00")
    private LocalDateTime lastCycleEndTime;
    
    @Schema(description = "上循环预计结束时间与实际结束时间的时间差（分钟），正数表示超时，负数表示节时", example = "-20")
    private Long lastCycleEndRemainingMinutes;

    @Schema(description = "上循环时间差中文描述，例如：\"超时30分钟\"、\"节省20分钟\"、\"按时完成\"", example = "超时30分钟")
    private String lastCycleEndRemainingText;
    
    @Schema(description = "本循环开始时间", example = "2025-11-05T08:00:00")
    private LocalDateTime currentCycleStartTime;
    
    @Schema(description = "本循环已进行时间（小时），用于显示已进行X小时", example = "30")
    private Long currentCycleElapsedHours;

    @Schema(description = "本循环已进行时间中文描述，例如：\"已进行3小时\"", example = "已进行3小时")
    private String currentCycleElapsedText;
    
    @Schema(description = "预计开始日期", example = "2025-11-05T08:00:00")
    private LocalDateTime estimatedStartDate;
    
    @Schema(description = "预计结束日期", example = "2025-11-05T14:00:00")
    private LocalDateTime estimatedEndDate;
    
    @Schema(description = "实际开始日期", example = "2025-11-05T08:00:00")
    private LocalDateTime actualStartDate;
    
    @Schema(description = "实际结束日期", example = "2025-11-05T14:00:00")
    private LocalDateTime actualEndDate;
    
    @Schema(description = "控制总时间（所有工序的控制时间总和，单位：小时），用于显示X/h", example = "5.0")
    private BigDecimal controlTotalTimeHours;
    
    @Schema(description = "工序列表")
    private List<ProcessInfo> processes;
    
    @Data
    @Schema(description = "工序信息")
    public static class ProcessInfo {
        @Schema(description = "工序ID", example = "1")
        private Long id;
        
        @Schema(description = "工序名称", example = "扒渣")
        private String name;
        
        @Schema(description = "控制时间（分钟）", example = "120")
        private Integer controlTime;
        
        @Schema(description = "实际时间（分钟）", example = "100")
        private Integer actualTime;
        
        @Schema(description = "工序状态", example = "COMPLETED")
        private String status;
        
        @Schema(description = "工序状态描述（中文）", example = "已完成")
        private String statusDesc;
        
        @Schema(description = "实际开始时间", example = "2025-11-05T08:00:00")
        private LocalDateTime actualStartTime;
        
        @Schema(description = "实际结束时间", example = "2025-11-05T09:40:00")
        private LocalDateTime actualEndTime;
        
        @Schema(description = "预计开始时间", example = "2025-11-05T08:00:00")
        private LocalDateTime estimatedStartTime;
        
        @Schema(description = "预计结束时间", example = "2025-11-05T10:00:00")
        private LocalDateTime estimatedEndTime;
        
        @Schema(description = "已进行时间（分钟）", example = "30")
        private Integer elapsedMinutes;
        
        @Schema(description = "时间差（分钟），正数表示超时，负数表示节时", example = "-20")
        private Integer timeDifferenceMinutes;
        
        @Schema(description = "时间差文本描述，例如：\"超时20分钟\"、\"节时15分钟\"、\"已进行30分钟\"", example = "节时20分钟")
        private String timeDifferenceText;
        
        @Schema(description = "是否结束工序", example = "false")
        private boolean endProcess;
    }
}

