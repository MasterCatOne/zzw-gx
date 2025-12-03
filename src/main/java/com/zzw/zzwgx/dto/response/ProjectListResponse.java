package com.zzw.zzwgx.dto.response;

import lombok.Data;

@Data
public class ProjectListResponse {
    
    private Long id;
    private Long parentId;
    private String nodeType;
    private String projectName;
    private String projectCode;
    private String projectStatus;
    private String statusDesc;
    /** 当前循环次数 */
    private Integer currentCycleNumber;
    /** 当前循环围岩等级 */
    private String rockLevel;
    private String rockLevelDesc;
}

