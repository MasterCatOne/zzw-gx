package com.zzw.zzwgx.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新建循环请求DTO
 */
@Data
public class CreateCycleRequest {
    
    @NotNull(message = "项目ID不能为空")
    private Long projectId;
    
    @NotNull(message = "控制时长标准不能为空")
    private Integer controlDuration;
    
    @NotNull(message = "开始日期不能为空")
    private LocalDateTime startDate;
    
    private LocalDateTime endDate;
    private LocalDateTime estimatedStartDate;
    private LocalDateTime estimatedEndDate;
    
    private Double estimatedMileage;
    private Double advanceLength;
    private String rockLevel;
    private String status;
    
    @NotNull(message = "工序模板ID不能为空")
    private Long templateId;
}

