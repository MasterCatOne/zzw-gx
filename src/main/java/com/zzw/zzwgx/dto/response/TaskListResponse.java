package com.zzw.zzwgx.dto.response;

import lombok.Data;

/**
 * 任务列表响应DTO
 */
@Data
public class TaskListResponse {
    
    private Long id;
    private String projectName;
    private String taskName;
    private String status;
    private String statusDesc;
    private Integer currentCycle;
    private Integer taskTime;
}

