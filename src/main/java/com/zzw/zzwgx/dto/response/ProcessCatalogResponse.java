package com.zzw.zzwgx.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工序字典响应DTO
 */
@Data
@Schema(description = "工序字典响应")
public class ProcessCatalogResponse {
    
    @Schema(description = "工序字典ID", example = "1")
    private Long id;
    
    @Schema(description = "工序名称", example = "扒渣（平整场地）")
    private String processName;
    
//    @Schema(description = "工序编码", example = "PROCESS_001")
//    private String processCode;
//
//    @Schema(description = "工序描述", example = "清理工作面，平整施工场地")
//    private String description;
//
//    @Schema(description = "显示顺序", example = "1")
//    private Integer displayOrder;
//
//    @Schema(description = "状态：0-禁用，1-启用", example = "1")
//    private Integer status;
//
//    @Schema(description = "创建时间", example = "2025-01-01 10:00:00")
//    private LocalDateTime createTime;
//
//    @Schema(description = "更新时间", example = "2025-01-01 10:00:00")
//    private LocalDateTime updateTime;
}

