package com.zzw.zzwgx.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工序模板响应DTO
 */
@Data
@Schema(description = "工序模板响应")
public class ProcessTemplateResponse {
    
    @Schema(description = "模板ID", example = "1")
    private Long id;
    
    @Schema(description = "模板名称", example = "标准模板")
    private String templateName;
    
    @Schema(description = "工序字典ID", example = "1")
    private Long processCatalogId;
    
    @Schema(description = "工序名称", example = "扒渣（平整场地）")
    private String processName;
    
    @Schema(description = "控制时间（分钟）", example = "120")
    private Integer controlTime;
    
    @Schema(description = "默认顺序", example = "1")
    private Integer defaultOrder;
    
    @Schema(description = "工序描述", example = "清理工作面，平整施工场地")
    private String description;
    
    @Schema(description = "创建时间", example = "2025-11-05T08:00:00")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间", example = "2025-11-05T08:00:00")
    private LocalDateTime updateTime;
}

