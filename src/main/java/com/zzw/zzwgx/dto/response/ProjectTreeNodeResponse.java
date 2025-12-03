package com.zzw.zzwgx.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 项目树节点响应
 */
@Data
public class ProjectTreeNodeResponse {
    
    private Long id;
    private Long parentId;
    private String nodeType;
    private String projectName;
    private String projectCode;
    private String projectDescription;
    private String projectStatus;
    private String statusDesc;
    private List<ProjectTreeNodeResponse> children = new ArrayList<>();
}

