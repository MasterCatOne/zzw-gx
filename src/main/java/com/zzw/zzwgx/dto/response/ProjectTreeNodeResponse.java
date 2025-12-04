package com.zzw.zzwgx.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 项目树节点响应
 */
@Data
@Schema(description = "项目树节点响应")
public class ProjectTreeNodeResponse {
    
    @Schema(description = "节点ID", example = "1")
    private Long id;
    
    @Schema(description = "父节点ID", example = "0")
    private Long parentId;
    
    @Schema(description = "节点类型", example = "SITE")
    private String nodeType;
    
    @Schema(description = "节点名称", example = "2号线2标段上行出口")
    private String projectName;
    
    @Schema(description = "节点编号", example = "SITE-105")
    private String projectCode;
    
    @Schema(description = "节点描述", example = "2号线2标段上行出口工点")
    private String projectDescription;
    
    @Schema(description = "项目状态", example = "IN_PROGRESS")
    private String projectStatus;
    
    @Schema(description = "状态描述（中文）", example = "进行中")
    private String statusDesc;
    
    @Schema(description = "子节点列表")
    private List<ProjectTreeNodeResponse> children = new ArrayList<>();
}

