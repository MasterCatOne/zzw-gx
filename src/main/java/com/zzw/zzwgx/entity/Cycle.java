package com.zzw.zzwgx.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 循环实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("cycle")
public class Cycle {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 项目ID
     */
    private Long projectId;
    
    /**
     * 循环次数
     */
    private Integer cycleNumber;
    
    /**
     * 控制时长标准（分钟）
     */
    private Integer controlDuration;
    
    /**
     * 开始日期
     */
    private LocalDateTime startDate;
    
    /**
     * 预估里程（米）
     */
    private BigDecimal estimatedMileage;
    
    /**
     * 循环状态：IN_PROGRESS-进行中，COMPLETED-已完成
     */
    private String status;
    
    /**
     * 循环进尺（米）
     */
    private BigDecimal advanceLength;
    
    /**
     * 工序模板ID
     */
    private Long templateId;
    
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

