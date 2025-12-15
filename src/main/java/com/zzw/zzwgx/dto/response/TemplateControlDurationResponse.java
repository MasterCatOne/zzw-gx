package com.zzw.zzwgx.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 模板控制时长响应DTO
 */
@Data
@Schema(description = "模板控制时长响应")
public class TemplateControlDurationResponse {
    
    @Schema(description = "模板ID", example = "1")
    private Long templateId;
    
    @Schema(description = "模板名称", example = "标准模板")
    private String templateName;
    
    @Schema(description = "控制时长标准（分钟），该模板下所有工序的控制时间总和", example = "1200")
    private Integer controlDuration;
}

