package com.zzw.zzwgx.dto.response;

import lombok.Data;

/**
 * 工点列表响应DTO
 * 只包含前端显示和操作所需的最小字段集
 */
@Data
public class ProjectListResponse {
    
    /** 工点ID，用于后续操作（查看详情等） */
    private Long id;
    
    /** 工点名称 */
    private String projectName;
    
    /** 项目状态 */
    private String projectStatus;
    
    /** 状态描述（中文） */
    private String statusDesc;
    
    /** 当前循环次数 */
    private Integer currentCycleNumber;
    
    /** 当前循环围岩等级 */
    private String rockLevel;
    
    /** 围岩等级描述（中文） */
    private String rockLevelDesc;
}

