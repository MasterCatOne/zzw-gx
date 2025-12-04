package com.zzw.zzwgx.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工序详情响应DTO
 */
@Data
@Schema(description = "工序详情响应")
public class ProcessDetailResponse {
    
    @Schema(description = "工序ID", example = "1")
    private Long id;
    
    @Schema(description = "工序名称", example = "扒渣")
    private String name;
    
    @Schema(description = "操作员姓名", example = "张三")
    private String operatorName;
    
    @Schema(description = "工序状态", example = "COMPLETED")
    private String status;
    
    @Schema(description = "工序状态描述（中文）", example = "已完成")
    private String statusDesc;
    
    @Schema(description = "所属循环号", example = "3")
    private Integer cycleNumber;

    @Schema(description = "控制时间（分钟）", example = "120")
    private Integer controlTime;
    
    @Schema(description = "工序实际耗时（分钟），若未完成则为已消耗时间", example = "115")
    private Integer processTimeMinutes;

    @Schema(description = "预计开始时间", example = "2025-11-05T08:00:00")
    private LocalDateTime estimatedStartTime;
    
    @Schema(description = "实际开始时间", example = "2025-11-05T08:00:00")
    private LocalDateTime actualStartTime;
    
    @Schema(description = "实际结束时间", example = "2025-11-05T09:40:00")
    private LocalDateTime actualEndTime;
    
    @Schema(description = "最终用时（分钟）", example = "100")
    private Integer finalTime;
    
    @Schema(description = "超时原因", example = "")
    private String overtimeReason;
    
    @Schema(description = "节省时间（分钟）", example = "20")
    private Integer savedTime;
    
    @Schema(description = "超时时间（分钟）", example = "0")
    private Integer overtime;

    @Schema(description = "上一工序名称", example = "钻爆")
    private String previousProcessName;

    @Schema(description = "上一工序状态：NOT_STARTED/IN_PROGRESS/COMPLETED", example = "COMPLETED")
    private String previousProcessStatus;

    @Schema(description = "上一工序状态描述", example = "已完成")
    private String previousProcessStatusDesc;
}

