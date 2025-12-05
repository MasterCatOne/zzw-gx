package com.zzw.zzwgx.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 更新工序顺序请求DTO
 */
@Data
@Schema(description = "批量更新工序顺序请求参数")
public class UpdateProcessOrderRequest {
    
    @Schema(description = "工序顺序列表，每个元素包含工序ID和新的顺序", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工序顺序列表不能为空")
    private List<ProcessOrderItem> processOrders;
    
    @Data
    @Schema(description = "工序顺序项")
    public static class ProcessOrderItem {
        @Schema(description = "工序ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        @NotNull(message = "工序ID不能为空")
        private Long processId;
        
        @Schema(description = "新的顺序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        @NotNull(message = "顺序不能为空")
        private Integer startOrder;
    }
}

