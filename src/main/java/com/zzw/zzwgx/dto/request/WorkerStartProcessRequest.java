package com.zzw.zzwgx.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 施工人员开始工序请求
 */
@Data
@Schema(description = "施工人员立即开始工序请求参数")
public class WorkerStartProcessRequest {

    @NotNull(message = "实际开始时间不能为空")
    @Schema(description = "选择的实际开始时间", example = "2025-11-05T08:30:00")
    private LocalDateTime actualStartTime;
}



