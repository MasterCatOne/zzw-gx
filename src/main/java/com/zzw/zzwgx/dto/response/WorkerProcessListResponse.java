package com.zzw.zzwgx.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 施工人员工序列表响应DTO
 */
@Data
@Schema(description = "施工人员工序列表项")
public class WorkerProcessListResponse {

    @Schema(description = "工序ID", example = "1")
    private Long processId;

    @Schema(description = "工点名称", example = "2号线2标段上行出口工点1")
    private String projectName;

    @Schema(description = "工序任务名称", example = "扒渣")
    private String processName;

    @Schema(description = "工序状态：NOT_STARTED/IN_PROGRESS/COMPLETED", example = "IN_PROGRESS")
    private String status;

    @Schema(description = "工序状态描述（中文）", example = "进行中")
    private String statusDesc;

    @Schema(description = "当前循环次数", example = "3")
    private Integer cycleNumber;

    @Schema(description = "任务时间（分钟），默认取控制时间controlTime", example = "120")
    private Integer taskTimeMinutes;

    @Schema(description = "工序实际开始时间", example = "2025-11-05T08:00:00")
    private LocalDateTime actualStartTime;

    @Schema(description = "工序实际结束时间", example = "2025-11-05T10:00:00")
    private LocalDateTime actualEndTime;
}


