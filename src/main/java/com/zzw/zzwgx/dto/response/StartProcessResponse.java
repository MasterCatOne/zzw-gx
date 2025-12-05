package com.zzw.zzwgx.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 开始工序响应DTO
 */
@Data
@Schema(description = "开始工序响应")
public class StartProcessResponse {

    @Schema(description = "是否成功", example = "true")
    private Boolean success;

    @Schema(description = "提示信息（如果上一工序未完成，会返回提示）", example = "上一工序'扒渣'尚未完成，请注意")
    private String warningMessage;

    @Schema(description = "上一工序名称", example = "扒渣")
    private String previousProcessName;

    @Schema(description = "上一工序状态", example = "IN_PROGRESS")
    private String previousProcessStatus;
}

