package com.zzw.zzwgx.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 循环报表数据响应
 */
@Data
public class CycleReportDataResponse {
    
    /**
     * 标题（A1单元格）
     */
    private String title;
    
    /**
     * 第2行数据
     */
    private Row2Data row2;
    
    /**
     * 第3行数据
     */
    private Row3Data row3;
    
    /**
     * 第4行数据
     */
    private Row4Data row4;
    
    /**
     * 第5行数据
     */
    private Row5Data row5;
    
    /**
     * 工序列表数据（从第8行开始）
     */
    private List<ProcessRowData> processList;
    
    /**
     * 合计行数据
     */
    private SummaryRowData summary;
    
    @Data
    public static class Row2Data {
        /**
         * C2：循环开始时间
         */
        private LocalDateTime cycleStartTime;
        
        /**
         * F2：循环结束时间（如果为null则显示"施工中"）
         */
        private LocalDateTime cycleEndTime;
        
        /**
         * K2：控制时长（分钟）
         */
        private Integer controlDurationMinutes;
        
        /**
         * L2：控制时长（小时）
         */
        private Double controlDurationHours;
        
        /**
         * O2：本月循环数
         */
        private Integer cycleNumber;
    }
    
    @Data
    public static class Row3Data {
        /**
         * C3：掌子面里程（默认"100.5"）
         */
        private String mileage;
        
        /**
         * F3：围岩等级
         */
        private String rockLevel;
        
        /**
         * K3：进尺
         */
        private String advanceLength;
        
        /**
         * O3：开发方式（固定"台阶法"）
         */
        private String developmentMethod;
    }
    
    @Data
    public static class Row4Data {
        /**
         * C4：上循环响炮时间（yyyy-MM-dd HH:mm:ss格式字符串）
         */
        private String lastCycleBlastTime;
        
        /**
         * F4：本循环理论响炮时间（yyyy-MM-dd HH:mm:ss格式字符串）
         */
        private String theoreticalBlastTime;
        
        /**
         * K4：响炮超时（"数据缺失"或"X天X小时X分钟"）
         */
        private String blastOvertime;
        
        /**
         * N4：两循环响炮时间差（分钟）
         */
        private Long cycleBlastDiffMinutes;
        
        /**
         * O4：两循环响炮时间差（X小时Y分钟）
         */
        private String cycleBlastDiffText;
    }
    
    @Data
    public static class Row5Data {
        /**
         * C5：循环开始时间（年月+时分）
         */
        private LocalDateTime cycleStartTime;
        
        /**
         * F5：预测下循环响炮时间（按控制标准，yyyy-MM-dd HH:mm:ss格式字符串）
         */
        private String predictedNextBlastTimeByControl;
        
        /**
         * K5：下循环响炮时间（按间隔，yyyy-MM-dd HH:mm:ss格式字符串）
         */
        private String predictedNextBlastTimeByInterval;
    }
    
    @Data
    public static class ProcessRowData {
        /**
         * A列：工序名称（如"1.扒渣"）
         */
        private String processName;
        
        /**
         * C列：开始年月（yyyy-MM）
         */
        private String startYearMonth;
        
        /**
         * D列：开始时分（HH:mm）
         */
        private String startTime;
        
        /**
         * E列：结束年月（yyyy-MM）
         */
        private String endYearMonth;
        
        /**
         * F列：结束时分（HH:mm）
         */
        private String endTime;
        
        /**
         * G列：耗时（分钟）
         */
        private Integer actualMinutes;
        
        /**
         * H列：耗时（X小时Y分钟）
         */
        private String actualTimeText;
        
        /**
         * I列：控制标准（分钟）
         */
        private Integer controlTime;
        
        /**
         * J列：控制标准（X小时Y分钟）
         */
        private String controlTimeText;
        
        /**
         * K列：控制标准和实际损耗差值（X小时Y分钟，负数表示节时）
         */
        private String diffText;
        
        /**
         * N列：情况说明
         */
        private String description;
        
        /**
         * O列：工序状态
         */
        private String status;

        /**
         * 是否超时（actualMinutes > controlTime 时为 true）
         */
        private Boolean overtime;

        /**
         * 工序所属大类（来自工序字典的 category，如 开挖 等）
         */
        private String category;
    }
    
    @Data
    public static class SummaryRowData {
        /**
         * G列：总耗时（分钟）
         */
        private Integer totalActualMinutes;
        
        /**
         * H列：总耗时（X小时Y分钟）
         */
        private String totalActualTimeText;
        
        /**
         * I列：总控制标准（分钟）
         */
        private Integer totalControlMinutes;
        
        /**
         * J列：总控制标准（X小时Y分钟）
         */
        private String totalControlTimeText;
        
        /**
         * K列：总差值（X小时Y分钟）
         */
        private String totalDiffText;

        /**
         * 总控制标准（小时），由 totalControlMinutes / 60 计算，保留两位小数
         */
        private java.math.BigDecimal totalControlHours;

        /**
         * 总实际耗时（小时），由 totalActualMinutes / 60 计算，保留两位小数
         */
        private java.math.BigDecimal totalActualHours;
    }
}

