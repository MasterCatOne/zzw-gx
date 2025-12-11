package com.zzw.zzwgx.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建工序字典请求DTO
 */
@Data
@Schema(description = "创建工序字典请求参数")
public class CreateProcessCatalogRequest {
    
    @Schema(description = "工序名称，必须唯一", requiredMode = Schema.RequiredMode.REQUIRED, example = "扒渣（平整场地）")
    @NotBlank(message = "工序名称不能为空")
    @Size(max = 100, message = "工序名称长度不能超过100个字符")
    private String processName;
    
    @Schema(description = "工序编码（可选，用于程序识别）", example = "PROCESS_001")
    @Size(max = 50, message = "工序编码长度不能超过50个字符")
    private String processCode;
    
    @Schema(description = "工序描述", example = "清理工作面，平整施工场地")
    @Size(max = 500, message = "工序描述长度不能超过500个字符")
    private String description;
    
    @Schema(description = "工序类别：EXCAVATION-开挖/HAULING-出渣/SUPPORT-立架/SHOTCRETE-喷砼", example = "EXCAVATION")
    @Size(max = 20, message = "工序类别长度不能超过20个字符")
    private String category;
    
    @Schema(description = "显示顺序（用于调整工序顺序）", example = "1")
    private Integer displayOrder;
    
    @Schema(description = "状态：0-禁用，1-启用，默认为1（启用）", example = "1")
    private Integer status;
}

