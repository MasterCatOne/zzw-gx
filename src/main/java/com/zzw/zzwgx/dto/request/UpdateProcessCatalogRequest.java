package com.zzw.zzwgx.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新工序字典请求DTO
 */
@Data
@Schema(description = "更新工序字典请求参数")
public class UpdateProcessCatalogRequest {
    
    @Schema(description = "工序名称（如果修改，需要确保唯一）", example = "扒渣（平整场地）")
    @Size(max = 100, message = "工序名称长度不能超过100个字符")
    private String processName;
    
    @Schema(description = "工序编码（可选，用于程序识别）", example = "PROCESS_001")
    @Size(max = 50, message = "工序编码长度不能超过50个字符")
    private String processCode;
    
    @Schema(description = "工序描述", example = "清理工作面，平整施工场地")
    @Size(max = 500, message = "工序描述长度不能超过500个字符")
    private String description;
    
    @Schema(description = "显示顺序（用于调整工序顺序）", example = "1")
    private Integer displayOrder;
    
    @Schema(description = "状态：0-禁用，1-启用", example = "1")
    private Integer status;
}

