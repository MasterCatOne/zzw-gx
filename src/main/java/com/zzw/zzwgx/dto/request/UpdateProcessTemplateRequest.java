package com.zzw.zzwgx.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 更新工序模板请求DTO
 */
@Data
@Schema(description = "更新工序模板请求参数")
public class UpdateProcessTemplateRequest {
    
    @Schema(description = "模板名称", example = "标准模板")
    private String templateName;
    
    @Schema(description = "工序字典ID（从工序字典表选择）", example = "1")
    private Long processCatalogId;
    
    @Schema(description = "工序名称（向后兼容，优先使用工序字典名称）", example = "扒渣（平整场地）")
    private String processName;
    
    @Schema(description = "控制时间（分钟）", example = "120")
    private Integer controlTime;
    
    @Schema(description = "默认顺序", example = "1")
    private Integer defaultOrder;
    
    @Schema(description = "工序描述", example = "清理工作面，平整施工场地")
    private String description;
}

