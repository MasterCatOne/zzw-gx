package com.zzw.zzwgx.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 循环响应DTO
 */
@Data
public class CycleResponse {
    
    private Long id;
    private Long projectId;
    private Integer cycleNumber;
    private Integer controlDuration;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime estimatedStartDate;
    private LocalDateTime estimatedEndDate;
    private BigDecimal estimatedMileage;
    private String status;
    private BigDecimal advanceLength;
    private String rockLevel;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

