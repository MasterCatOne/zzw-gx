package com.zzw.zzwgx.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 更新工序请求DTO
 */
@Data
@Schema(description = "更新工序请求参数")
public class UpdateProcessRequest {

    @Schema(description = "工序名称", example = "喷锚")
    private String name;

    @Schema(description = "控制时间（分钟），仅限管理员调整", example = "120")
    @Min(value = 0, message = "控制时间不能为负数")
    private Integer controlTime;

    @Schema(description = "预计开始时间", example = "2025-11-05T08:00:00")
    private LocalDateTime estimatedStartTime;

    @Schema(description = "预计结束时间", example = "2025-11-05T10:00:00")
    private LocalDateTime estimatedEndTime;

    @Schema(description = "实际开始时间", example = "2025-11-05T08:10:00")
    private LocalDateTime actualStartTime;

    @Schema(description = "实际结束时间", example = "2025-11-05T09:50:00")
    private LocalDateTime actualEndTime;

    @Schema(description = "工序状态：NOT_STARTED/IN_PROGRESS/COMPLETED", example = "IN_PROGRESS")
    private String status;

    @Schema(description = "操作员ID", example = "3")
    private Long operatorId;

    @Schema(description = "开始顺序", example = "2")
    private Integer startOrder;

    @Schema(description = "进尺长度（米）", example = "0.8")
    private BigDecimal advanceLength;

    @Schema(description = "实际里程（米），初喷后的测量放样结束后填报，用于更新循环的实际里程并自动计算进尺", example = "101.2")
    private BigDecimal actualMileage;

    @Schema(description = "超时原因（工序超时完成时需要填写，可在循环完成前填报）", example = "设备故障导致延误")
    private String overtimeReason;
}


