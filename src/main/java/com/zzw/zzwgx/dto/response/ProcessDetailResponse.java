package com.zzw.zzwgx.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工序详情响应DTO
 */
@Data
public class ProcessDetailResponse {
    
    private Long id;
    private String name;
    private String operatorName;
    private String status;
    private String statusDesc;
    private Integer controlTime;
    private LocalDateTime estimatedStartTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private Integer finalTime;
    private String overtimeReason;
    private Integer savedTime;
    private Integer overtime;
}

