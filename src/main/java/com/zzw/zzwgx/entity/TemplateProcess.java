package com.zzw.zzwgx.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 模板-工序关联实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("template_process")
public class TemplateProcess {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 模板ID（引用template表）
     */
    private Long templateId;
    
    /**
     * 工序字典ID（引用process_catalog表）
     */
    private Long processCatalogId;
    
    /**
     * 控制时间（分钟）
     */
    private Integer controlTime;
    
    /**
     * 默认顺序（在该模板中的顺序，唯一）
     */
    private Integer defaultOrder;
    
    /**
     * 工序描述（可选，如果为空则使用工序字典中的描述）
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

