package com.zzw.zzwgx.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 工点列表响应DTO
 * 只包含前端显示和操作所需的最小字段集
 */
@Data
@Schema(description = "工点列表响应")
public class ProjectListResponse {
    
    @Schema(description = "工点ID", example = "126")
    private Long id;
    
    @Schema(description = "工点名称", example = "2号线2标段上行出口")
    private String projectName;
    
    @Schema(description = "项目状态", example = "COMPLETED")
    private String projectStatus;
    
    @Schema(description = "状态描述（中文）", example = "已完成")
    private String statusDesc;
    
    @Schema(description = "当前循环次数", example = "3")
    private Integer currentCycleNumber;
    
    @Schema(description = "当前循环围岩等级", example = "LEVEL_II")
    private String rockLevel;
    
    @Schema(description = "围岩等级描述（中文）", example = "II级")
    private String rockLevelDesc;
}

