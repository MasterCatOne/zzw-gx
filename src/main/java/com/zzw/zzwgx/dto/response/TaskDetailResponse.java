package com.zzw.zzwgx.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务详情响应DTO
 */
@Data
public class TaskDetailResponse {
    
    private Long id;
    private String taskName;
    private String status;
    private String statusDesc;
    private Integer taskTime;
    private Integer currentCycle;
    private String previousProcess;
    private String previousProcessStatus;
    private LocalDateTime estimatedStartTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private Integer elapsedTime;
}

