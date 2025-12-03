package com.zzw.zzwgx.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工序实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("process")
public class Process {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 循环ID
     */
    private Long cycleId;
    
    /** 工序名称 */
    private String processName;
    
    /** 控制时间（分钟） */
    private Integer controlTime;
    
    /** 预计开始时间 */
    private LocalDateTime estimatedStartTime;
    
    /** 预计结束时间 */
    private LocalDateTime estimatedEndTime;
    
    /** 实际开始时间 */
    private LocalDateTime actualStartTime;
    
    /** 实际结束时间 */
    private LocalDateTime actualEndTime;
    
    /** 工序状态：NOT_STARTED/IN_PROGRESS/COMPLETED */
    private String processStatus;
    
    /**
     * 操作员ID
     */
    private Long operatorId;
    
    /**
     * 开始顺序
     */
    private Integer startOrder;
    
    /**
     * 进尺长度（米）
     */
    private BigDecimal advanceLength;
    
    /**
     * 删除标志：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

