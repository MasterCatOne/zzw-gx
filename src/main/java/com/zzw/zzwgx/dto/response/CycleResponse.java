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
    private BigDecimal estimatedMileage;
    private String status;
    private BigDecimal advanceLength;
    private Long templateId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

