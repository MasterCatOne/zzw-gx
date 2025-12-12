package com.zzw.zzwgx.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建工序模板请求DTO
 */
@Data
@Schema(description = "创建工序模板请求参数")
public class CreateProcessTemplateRequest {
    
    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "标准模板")
    @NotBlank(message = "模板名称不能为空")
    private String templateName;
    
    @Schema(description = "工点ID（必填，从工点列表中选择）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "工点ID不能为空")
    private Long siteId;
    
    @Schema(description = "工序字典ID（从工序字典表选择）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "工序字典ID不能为空")
    private Long processCatalogId;
    
    @Schema(description = "工序名称（向后兼容，优先使用工序字典名称）", example = "扒渣（平整场地）")
    private String processName;
    
    @Schema(description = "控制时间（分钟）", requiredMode = Schema.RequiredMode.REQUIRED, example = "120")
    @NotNull(message = "控制时间不能为空")
    private Integer controlTime;
    
    @Schema(description = "默认顺序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "默认顺序不能为空")
    private Integer defaultOrder;
    
    @Schema(description = "工序描述", example = "清理工作面，平整施工场地")
    private String description;
}

