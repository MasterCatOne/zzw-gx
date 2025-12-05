package com.zzw.zzwgx.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工序字典实体类（统一管理所有工序，支持顺序调整）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("process_catalog")
public class ProcessCatalog {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 工序名称（唯一）
     */
    private String processName;
    
    /**
     * 工序编码（可选，用于程序识别）
     */
    private String processCode;
    
    /**
     * 工序描述
     */
    private String description;
    
    /**
     * 显示顺序（用于调整工序顺序）
     */
    private Integer displayOrder;
    
    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;
    
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

