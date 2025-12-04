package com.zzw.zzwgx.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 进度详情响应DTO
 */
@Data
public class ProgressDetailResponse {
    
    private Long cycleId;
    private Integer cycleNumber;
    private String cycleStatus;
    private String cycleStatusDesc;
    private Integer controlDuration;
    private BigDecimal advanceLength;
    private String currentProcess;
    private LocalDateTime lastCycleEndTime;
    /** 上循环结束到本循环开始的时间差（分钟），用于显示"余时" */
    private Long lastCycleEndRemainingMinutes;
    private LocalDateTime currentCycleStartTime;
    /** 本循环已进行时间（小时），用于显示"已进行X小时" */
    private Long currentCycleElapsedHours;
    private LocalDateTime estimatedStartDate;
    private LocalDateTime estimatedEndDate;
    private LocalDateTime actualStartDate;
    private LocalDateTime actualEndDate;
    /** 控制总时间（所有工序的控制时间总和，单位：小时），用于显示"X/h" */
    private BigDecimal controlTotalTimeHours;
    private List<ProcessInfo> processes;
    
    @Data
    public static class ProcessInfo {
        private Long id;
        private String name;
        private Integer controlTime;
        private Integer actualTime;
        private String status;
        private String statusDesc;
        private LocalDateTime actualStartTime;
        private LocalDateTime actualEndTime;
        private LocalDateTime estimatedStartTime;
        private LocalDateTime estimatedEndTime;
        private Integer elapsedMinutes;
        private Integer timeDifferenceMinutes;
        private boolean endProcess;
    }
}

