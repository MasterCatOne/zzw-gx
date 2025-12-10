package com.zzw.zzwgx.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 循环响应DTO
 */
@Data
@Schema(description = "循环响应")
public class CycleResponse {
    
    @Schema(description = "循环ID", example = "1")
    private Long id;
    
    @Schema(description = "项目ID", example = "126")
    private Long projectId;
    
    @Schema(description = "循环次数", example = "3")
    private Integer cycleNumber;
    
    @Schema(description = "控制时长标准（分钟）", example = "360")
    private Integer controlDuration;
    
    @Schema(description = "开始日期", example = "2025-11-05T08:00:00")
    private LocalDateTime startDate;
    
    @Schema(description = "结束日期", example = "2025-11-05T14:00:00")
    private LocalDateTime endDate;
    
    @Schema(description = "预计开始日期", example = "2025-11-05T08:00:00")
    private LocalDateTime estimatedStartDate;
    
    @Schema(description = "预计结束日期", example = "2025-11-05T14:00:00")
    private LocalDateTime estimatedEndDate;
    
    @Schema(description = "预估里程（米）", example = "1.5")
    private BigDecimal estimatedMileage;
    
    @Schema(description = "实际里程（米），初喷后的测量放样结束后填报", example = "1.8")
    private BigDecimal actualMileage;

    @Schema(description = "开挖/开发方式，如：台阶法", example = "台阶法")
    private String developmentMethod;
    
    @Schema(description = "循环状态", example = "IN_PROGRESS")
    private String status;
    
    @Schema(description = "循环进尺（米）", example = "0.5")
    private BigDecimal advanceLength;
    
    @Schema(description = "围岩等级", example = "LEVEL_II")
    private String rockLevel;
    
    @Schema(description = "创建时间", example = "2025-11-05T08:00:00")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间", example = "2025-11-05T08:00:00")
    private LocalDateTime updateTime;
}

