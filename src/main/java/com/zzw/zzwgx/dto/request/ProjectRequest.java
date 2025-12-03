package com.zzw.zzwgx.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新建/编辑项目树节点请求
 */
@Data
public class ProjectRequest {
    
    @Schema(description = "父节点ID，根节点可为空")
    private Long parentId;
    
    @Schema(description = "节点类型：PROJECT/SECTION/TUNNEL/SITE 等", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "节点类型不能为空")
    private String nodeType;
    
    @Schema(description = "节点名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "节点名称不能为空")
    @Size(max = 100, message = "节点名称长度不能超过100个字符")
    private String projectName;
    
    @Schema(description = "节点编码，需要唯一")
    @Size(max = 50, message = "节点编码长度不能超过50个字符")
    private String projectCode;
    
    @Schema(description = "节点描述")
    private String projectDescription;
    
    @Schema(description = "节点状态：IN_PROGRESS/COMPLETED/PAUSED", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "节点状态不能为空")
    private String projectStatus;
}

