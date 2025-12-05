package com.zzw.zzwgx.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量更新工序顺序请求DTO
 */
@Data
@Schema(description = "批量更新工序顺序请求参数")
public class UpdateProcessCatalogOrderRequest {
    
    @Schema(description = "工序顺序列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "工序顺序列表不能为空")
    @Valid
    private List<ProcessOrderItem> orders;
    
    @Data
    @Schema(description = "工序顺序项")
    public static class ProcessOrderItem {
        @Schema(description = "工序字典ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        private Long catalogId;
        
        @Schema(description = "新的显示顺序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        private Integer displayOrder;
    }
}

