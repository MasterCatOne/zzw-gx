package com.zzw.zzwgx.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务详情响应DTO
 */
@Data
@Schema(description = "任务详情响应")
public class TaskDetailResponse {
    
    @Schema(description = "任务ID", example = "1")
    private Long id;
    
    @Schema(description = "任务名称", example = "扒渣")
    private String taskName;
    
    @Schema(description = "任务状态", example = "IN_PROGRESS")
    private String status;
    
    @Schema(description = "任务状态描述（中文）", example = "进行中")
    private String statusDesc;
    
    @Schema(description = "任务时间（分钟）", example = "120")
    private Integer taskTime;
    
    @Schema(description = "当前循环次数", example = "3")
    private Integer currentCycle;
    
    @Schema(description = "上一工序名称", example = "放样测量")
    private String previousProcess;
    
    @Schema(description = "上一工序状态", example = "COMPLETED")
    private String previousProcessStatus;
    
    @Schema(description = "预计开始时间", example = "2025-11-05T08:00:00")
    private LocalDateTime estimatedStartTime;
    
    @Schema(description = "实际开始时间", example = "2025-11-05T08:00:00")
    private LocalDateTime actualStartTime;
    
    @Schema(description = "实际结束时间", example = "2025-11-05T10:00:00")
    private LocalDateTime actualEndTime;
    
    @Schema(description = "已用时间（分钟）", example = "30")
    private Integer elapsedTime;
}

