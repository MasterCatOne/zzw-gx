package com.zzw.zzwgx.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工点施工状态响应DTO（管理员查看）
 */
@Data
@Schema(description = "工点施工状态响应，用于管理员查看各工点的实时施工状态")
public class SiteConstructionStatusResponse {
    
    @Schema(description = "工点ID", example = "1")
    private Long projectId;
    
    @Schema(description = "工点名称", example = "工点1")
    private String projectName;
    
    @Schema(description = "工点状态", example = "IN_PROGRESS")
    private String projectStatus;
    
    @Schema(description = "当前循环ID", example = "10")
    private Long cycleId;
    
    @Schema(description = "当前循环号", example = "3")
    private Integer cycleNumber;
    
    @Schema(description = "当前循环状态", example = "IN_PROGRESS")
    private String cycleStatus;
    
    @Schema(description = "当前循环状态描述", example = "进行中")
    private String cycleStatusDesc;
    
    @Schema(description = "当前正在进行的工序信息")
    private CurrentProcessInfo currentProcess;
    
    @Schema(description = "已完成的工序列表（上几道工序）")
    private List<CompletedProcessInfo> completedProcesses;
    
    @Schema(description = "当前循环开始时间", example = "2025-11-05T08:00:00")
    private LocalDateTime cycleStartTime;
    
    @Schema(description = "当前循环已进行时长（分钟）", example = "180")
    private Long cycleElapsedMinutes;
    
    @Schema(description = "当前循环已进行时长描述", example = "已进行3小时")
    private String cycleElapsedText;
    
    @Data
    @Schema(description = "当前正在进行的工序信息")
    public static class CurrentProcessInfo {
        @Schema(description = "工序ID", example = "100")
        private Long processId;
        
        @Schema(description = "工序名称", example = "扒渣")
        private String processName;
        
        @Schema(description = "工序开始时间", example = "2025-11-05T10:00:00")
        private LocalDateTime startTime;
        
        @Schema(description = "工序持续时长（分钟）", example = "120")
        private Long durationMinutes;
        
        @Schema(description = "工序持续时长描述", example = "已进行2小时")
        private String durationText;
        
        @Schema(description = "控制时间（分钟）", example = "180")
        private Integer controlTime;
        
        @Schema(description = "操作员姓名", example = "张三")
        private String operatorName;
        
        @Schema(description = "是否超时", example = "false")
        private Boolean isOvertime;
        
        @Schema(description = "超时/节时描述", example = "已进行2小时，剩余1小时")
        private String timeStatusText;
    }
    
    @Data
    @Schema(description = "已完成的工序信息")
    public static class CompletedProcessInfo {
        @Schema(description = "工序ID", example = "99")
        private Long processId;
        
        @Schema(description = "工序名称", example = "初喷")
        private String processName;
        
        @Schema(description = "工序开始时间", example = "2025-11-05T08:00:00")
        private LocalDateTime startTime;
        
        @Schema(description = "工序结束时间", example = "2025-11-05T10:00:00")
        private LocalDateTime endTime;
        
        @Schema(description = "控制时间（分钟）", example = "120")
        private Integer controlTime;
        
        @Schema(description = "实际耗时（分钟）", example = "100")
        private Integer actualTime;
        
        @Schema(description = "时间差（分钟），正数表示超时，负数表示节时", example = "-20")
        private Integer timeDifference;
        
        @Schema(description = "节超情况描述", example = "节省20分钟")
        private String timeStatusText;
        
        @Schema(description = "操作员姓名", example = "李四")
        private String operatorName;
        
        @Schema(description = "是否超时", example = "false")
        private Boolean isOvertime;
        
        @Schema(description = "超时原因（如果有）", example = "设备故障")
        private String overtimeReason;
    }
}

