package com.zzw.zzwgx.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工序响应DTO
 */
@Data
@Schema(description = "工序响应")
public class ProcessResponse {
    
    @Schema(description = "工序ID", example = "1")
    private Long id;
    
    @Schema(description = "循环ID", example = "1")
    private Long cycleId;
    
    @Schema(description = "工序名称", example = "扒渣")
    private String name;
    
    @Schema(description = "控制时间（分钟）", example = "120")
    private Integer controlTime;
    
    @Schema(description = "工序状态：NOT_STARTED/IN_PROGRESS/COMPLETED", example = "NOT_STARTED")
    private String status;

    @Schema(description = "工序状态描述（中文）", example = "未开始")
    private String statusDesc;
    
    @Schema(description = "操作员ID", example = "1")
    private Long operatorId;

    @Schema(description = "操作员姓名", example = "张三")
    private String operatorName;
    
    @Schema(description = "开始顺序", example = "1")
    private Integer startOrder;
    
    @Schema(description = "进尺长度（米）", example = "0.5")
    private BigDecimal advanceLength;
    
    @Schema(description = "工序字典ID", example = "1")
    private Long processCatalogId;
    
    @Schema(description = "预计开始时间", example = "2025-11-05T08:00:00")
    private LocalDateTime estimatedStartTime;

    @Schema(description = "预计结束时间", example = "2025-11-05T10:00:00")
    private LocalDateTime estimatedEndTime;
    
    @Schema(description = "实际开始时间", example = "2025-11-05T08:00:00")
    private LocalDateTime actualStartTime;
    
    @Schema(description = "实际结束时间", example = "2025-11-05T10:00:00")
    private LocalDateTime actualEndTime;
    
    @Schema(description = "创建时间", example = "2025-11-05T08:00:00")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间", example = "2025-11-05T08:00:00")
    private LocalDateTime updateTime;
}

