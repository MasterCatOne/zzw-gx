package com.zzw.zzwgx.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 补填工序时间请求DTO
 */
@Data
@Schema(description = "补填工序时间请求参数")
public class FillProcessTimeRequest {

    @Schema(description = "实际开始时间（可选，不填则使用预计开始时间）", example = "2025-12-22T08:00:00")
    private LocalDateTime actualStartTime;

    @Schema(description = "实际结束时间（可选，不填则使用预计结束时间）", example = "2025-12-22T10:00:00")
    private LocalDateTime actualEndTime;
}

