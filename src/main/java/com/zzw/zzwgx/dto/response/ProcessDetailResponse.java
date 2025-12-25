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
    
    @Schema(description = "超时原因", example = "")
    private String overtimeReason;

    @Schema(description = "时间差文本描述，例如：\"超时20分钟\"、\"节时15分钟\"、\"已进行30分钟\"", example = "节时20分钟")
    private String timeDifferenceText;

    @Schema(description = "上一工序名称", example = "钻爆")
    private String previousProcessName;

    @Schema(description = "上一工序状态：NOT_STARTED/IN_PROGRESS/COMPLETED", example = "COMPLETED")
    private String previousProcessStatus;

    @Schema(description = "上一工序状态描述", example = "已完成")
    private String previousProcessStatusDesc;
    
    @Schema(description = "预计结束时间", example = "2025-11-05T10:00:00")
    private LocalDateTime estimatedEndTime;
    
    @Schema(description = "是否需要补填时间（当前系统时间超过预计完成时间时需要补填）", example = "false")
    private Boolean needsTimeFill;
}

