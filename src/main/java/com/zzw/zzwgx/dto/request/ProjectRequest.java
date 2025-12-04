package com.zzw.zzwgx.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新建/编辑项目树节点请求
 */
@Data
@Schema(description = "创建或更新项目树节点请求参数")
public class ProjectRequest {
    
    @Schema(description = "父节点ID，根节点可为空", example = "1")
    private Long parentId;
    
    @Schema(description = "节点类型：PROJECT（项目）/SECTION（标段）/TUNNEL（隧道）/SITE（工点）等", requiredMode = Schema.RequiredMode.REQUIRED, example = "SITE")
    @NotBlank(message = "节点类型不能为空")
    private String nodeType;
    
    @Schema(description = "节点名称，最大长度100个字符", requiredMode = Schema.RequiredMode.REQUIRED, example = "工点1")
    @NotBlank(message = "节点名称不能为空")
    @Size(max = 100, message = "节点名称长度不能超过100个字符")
    private String projectName;
    
    @Schema(description = "节点编码，需要唯一，最大长度50个字符", example = "SITE001")
    @Size(max = 50, message = "节点编码长度不能超过50个字符")
    private String projectCode;
    
    @Schema(description = "节点描述信息", example = "这是工点1的描述")
    private String projectDescription;
    
    @Schema(description = "节点状态：IN_PROGRESS（进行中）/COMPLETED（已完成）/PAUSED（已暂停）", requiredMode = Schema.RequiredMode.REQUIRED, example = "IN_PROGRESS")
    @NotBlank(message = "节点状态不能为空")
    private String projectStatus;
}

