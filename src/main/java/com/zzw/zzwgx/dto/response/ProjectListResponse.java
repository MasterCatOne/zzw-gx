package com.zzw.zzwgx.dto.response;

import lombok.Data;

/**
 * 项目列表响应DTO
 */
@Data
public class ProjectListResponse {
    
    private Long id;
    private String name;
    private String status;
    private String statusDesc;
    private Integer currentCycle;
    private String rockLevel;
    private String rockLevelDesc;
}

