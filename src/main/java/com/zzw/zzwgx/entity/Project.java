package com.zzw.zzwgx.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("project")
public class Project {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 父节点ID，为空表示顶级节点
     */
    private Long parentId;
    
    /**
     * 节点类型：PROJECT/SECTION/TUNNEL/SITE 等
     */
    private String nodeType;
    
    /**
     * 节点名称
     */
    @TableField("project_name")
    private String projectName;
    
    /**
     * 节点编码
     */
    @TableField("project_code")
    private String projectCode;
    
    /**
     * 节点描述
     */
    @TableField("project_description")
    private String projectDescription;
    
    /**
     * 节点状态：IN_PROGRESS / COMPLETED / PAUSED
     */
    @TableField("project_status")
    private String projectStatus;
    
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

