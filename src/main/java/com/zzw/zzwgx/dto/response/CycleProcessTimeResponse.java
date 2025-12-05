package com.zzw.zzwgx.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 循环工序时间统计响应DTO
 */
@Data
@Schema(description = "循环工序时间统计响应")
public class CycleProcessTimeResponse {

    @Schema(description = "循环ID", example = "1")
    private Long cycleId;

    @Schema(description = "循环号", example = "3")
    private Integer cycleNumber;

    @Schema(description = "工序总数", example = "14")
    private Integer totalProcessCount;

    @Schema(description = "已完成工序数", example = "10")
    private Integer completedProcessCount;

    @Schema(description = "单工序总时间（分钟），所有工序实际完成时间的总和（不考虑重叠）", example = "1680")
    private Long totalIndividualTimeMinutes;

    @Schema(description = "整套工序总时间（分钟），考虑重叠时间不重复计算", example = "1200")
    private Long totalCycleTimeMinutes;

    @Schema(description = "重叠时间（分钟）", example = "480")
    private Long overlapTimeMinutes;

    @Schema(description = "工序时间详情列表")
    private List<ProcessTimeDetail> processDetails;

    @Data
    @Schema(description = "单个工序时间详情")
    public static class ProcessTimeDetail {
        @Schema(description = "工序ID", example = "1")
        private Long processId;

        @Schema(description = "工序名称", example = "扒渣")
        private String processName;

        @Schema(description = "实际开始时间", example = "2025-11-05T08:00:00")
        private java.time.LocalDateTime actualStartTime;

        @Schema(description = "实际结束时间", example = "2025-11-05T10:00:00")
        private java.time.LocalDateTime actualEndTime;

        @Schema(description = "工序实际耗时（分钟）", example = "120")
        private Long processTimeMinutes;
    }
}

