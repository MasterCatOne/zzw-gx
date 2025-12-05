package com.zzw.zzwgx.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 填报超时原因请求DTO
 */
@Data
@Schema(description = "填报超时原因请求参数")
public class SubmitOvertimeReasonRequest {

    @Schema(description = "超时原因（必填）", requiredMode = Schema.RequiredMode.REQUIRED, example = "设备故障导致延误")
    @NotBlank(message = "超时原因不能为空")
    private String overtimeReason;
}

