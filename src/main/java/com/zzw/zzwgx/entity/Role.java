package com.zzw.zzwgx.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 角色实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_role")
public class Role {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    @TableField("role_code")
    private String roleCode;
    
    @TableField("role_description")
    private String roleDescription;
    
    @TableField("role_status")
    private Integer roleStatus;
    
    @TableLogic
    private Integer deleted;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

