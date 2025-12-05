package com.zzw.zzwgx.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新建工序请求DTO
 */
@Data
@Schema(description = "创建新工序请求参数")
public class CreateProcessRequest {
    
    @Schema(description = "所属循环ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "循环ID不能为空")
    private Long cycleId;
    
    @Schema(description = "控制时长标准（分钟），必填，由用户输入", requiredMode = Schema.RequiredMode.REQUIRED, example = "120")
    @NotNull(message = "控制时长标准不能为空")
    private Integer controlTime;
    
    @Schema(description = "实际开始时间", example = "2024-01-01T08:00:00")
    private LocalDateTime actualStartTime;
    
    @Schema(description = "施工人员用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    @NotNull(message = "施工人员ID不能为空")
    private Long workerId;
    
    @Schema(description = "开始顺序，用于工序执行顺序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "开始顺序不能为空")
    private Integer startOrder;
    
    @Schema(description = "工序模板ID（可选，如果指定则记录工序来源模板）", example = "1")
    private Long templateId;
}

