package com.zzw.zzwgx.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 项目实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("project")
public class Project {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 工点名称
     */
    private String name;
    
    /**
     * 项目状态：IN_PROGRESS-进行中，COMPLETED-已完成，PAUSED-已暂停
     */
    private String status;
    
    /**
     * 围岩等级：LEVEL_I-I级，LEVEL_II-II级
     */
    private String rockLevel;
    
    /**
     * 当前循环次数
     */
    private Integer currentCycle;
    
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

