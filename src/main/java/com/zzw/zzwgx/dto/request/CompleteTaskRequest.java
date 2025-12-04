package com.zzw.zzwgx.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 完成任务请求DTO
 */
@Data
@Schema(description = "完成任务请求参数")
public class CompleteTaskRequest {
    
    @Schema(description = "超时原因说明（如果任务超时完成，需要填写原因）", example = "设备故障导致延误")
    private String overtimeReason;
}

