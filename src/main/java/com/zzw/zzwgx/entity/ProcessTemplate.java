package com.zzw.zzwgx.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工序模板实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("process_template")
public class ProcessTemplate {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 模板名称
     */
    private String templateName;
    
    /**
     * 工序字典ID（引用process_catalog表）
     */
    private Long processCatalogId;
    
    /**
     * 工序名称（保留字段，用于向后兼容，实际应从process_catalog表关联获取）
     */
    private String processName;
    
    /**
     * 控制时间（分钟）
     */
    private Integer controlTime;
    
    /**
     * 默认顺序
     */
    private Integer defaultOrder;
    
    /**
     * 工序描述
     */
    private String description;
    
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

