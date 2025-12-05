package com.zzw.zzwgx.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 超时未填报原因工序响应DTO
 */
@Data
@Schema(description = "超时未填报原因工序信息")
public class OvertimeProcessResponse {

    @Schema(description = "工序ID", example = "1")
    private Long processId;

    @Schema(description = "工序名称", example = "扒渣")
    private String processName;

    @Schema(description = "工点名称", example = "2号线2标段上行出口工点1")
    private String projectName;

    @Schema(description = "循环号", example = "3")
    private Integer cycleNumber;

    @Schema(description = "操作员姓名", example = "张三")
    private String operatorName;

    @Schema(description = "控制时间（分钟）", example = "120")
    private Integer controlTime;

    @Schema(description = "实际开始时间", example = "2025-11-05T08:00:00")
    private LocalDateTime actualStartTime;

    @Schema(description = "实际结束时间", example = "2025-11-05T10:30:00")
    private LocalDateTime actualEndTime;

    @Schema(description = "实际耗时（分钟）", example = "150")
    private Long actualTimeMinutes;

    @Schema(description = "超时时间（分钟）", example = "30")
    private Long overtimeMinutes;
}

