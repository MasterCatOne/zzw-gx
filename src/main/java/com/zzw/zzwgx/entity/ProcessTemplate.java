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
    private String name;
    
    /**
     * 工序列表（JSON格式）
     */
    private String processList;
    
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

