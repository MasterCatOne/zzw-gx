package com.zzw.zzwgx.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 更新循环请求
 */
@Data
@Schema(description = "更新循环信息请求参数")
public class UpdateCycleRequest {
    
    @Schema(description = "控制时长（分钟）", example = "480")
    @Min(value = 0, message = "控制时长不能为负数")
    private Integer controlDuration;
    
    @Schema(description = "实际开始时间", example = "2024-01-01T08:00:00")
    private LocalDateTime startDate;
    
    @Schema(description = "实际结束时间", example = "2024-01-01T16:00:00")
    private LocalDateTime endDate;
    
    @Schema(description = "预估开始时间", example = "2024-01-01T08:00:00")
    private LocalDateTime estimatedStartDate;
    
    @Schema(description = "预估结束时间", example = "2024-01-01T16:00:00")
    private LocalDateTime estimatedEndDate;
    
    @Schema(description = "预计里程（米）", example = "2.5")
    private BigDecimal estimatedMileage;
    
    @Schema(description = "实际里程（米），初喷后的测量放样结束后填报", example = "2.8")
    private BigDecimal actualMileage;
    
    @Schema(description = "实际进尺（米）", example = "2.3")
    private BigDecimal advanceLength;

    @Schema(description = "开挖/开发方式，如：台阶法", example = "台阶法")
    private String developmentMethod;
    
    @Schema(description = "循环状态：IN_PROGRESS/COMPLETED/PAUSED", example = "IN_PROGRESS")
    private String status;
    
    @Schema(description = "围岩等级，如：III、IV、V等", example = "III")
    private String rockLevel;
}

