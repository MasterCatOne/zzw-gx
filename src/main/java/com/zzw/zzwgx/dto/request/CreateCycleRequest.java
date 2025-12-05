package com.zzw.zzwgx.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新建循环请求DTO
 */
@Data
@Schema(description = "创建新循环请求参数")
public class CreateCycleRequest {
    
    @Schema(description = "工点项目ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "项目ID不能为空")
    private Long projectId;
    
    @Schema(description = "控制时长标准（分钟）", requiredMode = Schema.RequiredMode.REQUIRED, example = "480")
    @NotNull(message = "控制时长标准不能为空")
    private Integer controlDuration;
    
    @Schema(description = "实际开始时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01T08:00:00")
    @NotNull(message = "开始日期不能为空")
    private LocalDateTime startDate;
    
    @Schema(description = "实际结束时间", example = "2024-01-01T16:00:00")
    private LocalDateTime endDate;
    
    @Schema(description = "预估开始时间（如果不提供，将自动设置为与实际开始时间一致）", example = "2024-01-01T08:00:00")
    private LocalDateTime estimatedStartDate;
    
    @Schema(description = "预估结束时间（如果不提供，将根据开始时间和控制时长自动计算）", example = "2024-01-01T16:00:00")
    private LocalDateTime estimatedEndDate;
    
    @Schema(description = "预计里程（米）", example = "2.5")
    private Double estimatedMileage;
    
    @Schema(description = "实际进尺（米）", example = "2.3")
    private Double advanceLength;
    
    @Schema(description = "围岩等级，如：III、IV、V等", example = "III")
    private String rockLevel;
    
    @Schema(description = "循环状态：IN_PROGRESS/COMPLETED/PAUSED", example = "IN_PROGRESS")
    private String status;
    
    @Schema(description = "工序模板ID（传入该模板下任意一个工序模板的ID即可，后端会根据模板名称获取该模板下的所有工序）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "工序模板ID不能为空")
    private Long templateId;
}

