package com.zzw.zzwgx.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工序响应DTO
 */
@Data
public class ProcessResponse {
    
    private Long id;
    private Long cycleId;
    private String name;
    private Integer controlTime;
    private String status;
    private Long operatorId;
    private Integer startOrder;
    private BigDecimal advanceLength;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

