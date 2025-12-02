package com.zzw.zzwgx.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新建工序请求DTO
 */
@Data
public class CreateProcessRequest {
    
    @NotNull(message = "循环ID不能为空")
    private Long cycleId;
    
    @NotBlank(message = "工序名称不能为空")
    private String name;
    
    @NotNull(message = "控制时长标准不能为空")
    private Integer controlTime;
    
    private LocalDateTime actualStartTime;
    
    @NotNull(message = "施工人员ID不能为空")
    private Long workerId;
    
    @NotNull(message = "开始顺序不能为空")
    private Integer startOrder;
}

