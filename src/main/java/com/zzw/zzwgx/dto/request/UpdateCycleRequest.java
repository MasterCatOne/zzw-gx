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
public class UpdateCycleRequest {
    
    @Schema(description = "控制时长（分钟）")
    @Min(value = 0, message = "控制时长不能为负数")
    private Integer controlDuration;
    
    @Schema(description = "开始日期")
    private LocalDateTime startDate;
    
    @Schema(description = "结束日期")
    private LocalDateTime endDate;
    
    @Schema(description = "预计开始日期")
    private LocalDateTime estimatedStartDate;
    
    @Schema(description = "预计结束日期")
    private LocalDateTime estimatedEndDate;
    
    @Schema(description = "预计里程（米）")
    private BigDecimal estimatedMileage;
    
    @Schema(description = "实际进尺（米）")
    private BigDecimal advanceLength;
    
    @Schema(description = "循环状态：IN_PROGRESS/COMPLETED")
    private String status;
    
    @Schema(description = "围岩等级")
    private String rockLevel;
}

