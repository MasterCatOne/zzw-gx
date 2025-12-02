package com.zzw.zzwgx.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 任务实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("task")
public class Task {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 工序ID
     */
    private Long processId;
    
    /**
     * 施工人员ID
     */
    private Long workerId;
    
    /**
     * 任务状态：PENDING-待完成，IN_PROGRESS-进行中，COMPLETED-已完成
     */
    private String status;
    
    /**
     * 预计开始时间
     */
    private LocalDateTime estimatedStartTime;
    
    /**
     * 实际开始时间
     */
    private LocalDateTime actualStartTime;
    
    /**
     * 实际结束时间
     */
    private LocalDateTime actualEndTime;
    
    /**
     * 超时原因
     */
    private String overtimeReason;
    
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

