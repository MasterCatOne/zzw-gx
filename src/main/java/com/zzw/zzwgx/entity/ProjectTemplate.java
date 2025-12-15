package com.zzw.zzwgx.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工点-模板关联实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("project_template")
public class ProjectTemplate {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 工点ID（引用project表，node_type必须为SITE）
     */
    private Long projectId;
    
    /**
     * 模板ID（引用template表）
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

