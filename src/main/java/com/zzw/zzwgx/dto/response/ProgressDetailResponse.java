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
    private LocalDateTime currentCycleStartTime;
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
    }
}

