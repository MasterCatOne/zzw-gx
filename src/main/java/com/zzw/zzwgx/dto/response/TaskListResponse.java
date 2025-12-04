package com.zzw.zzwgx.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 任务列表响应DTO
 */
@Data
@Schema(description = "任务列表响应")
public class TaskListResponse {
    
    @Schema(description = "任务ID", example = "1")
    private Long id;
    
    @Schema(description = "项目名称", example = "2号线2标段上行出口")
    private String projectName;
    
    @Schema(description = "任务名称", example = "扒渣")
    private String taskName;
    
    @Schema(description = "任务状态", example = "IN_PROGRESS")
    private String status;
    
    @Schema(description = "任务状态描述（中文）", example = "进行中")
    private String statusDesc;
    
    @Schema(description = "当前循环次数", example = "3")
    private Integer currentCycle;
    
    @Schema(description = "任务时间（分钟）", example = "120")
    private Integer taskTime;
}

