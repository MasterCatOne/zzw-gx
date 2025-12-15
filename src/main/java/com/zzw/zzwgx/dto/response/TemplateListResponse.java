package com.zzw.zzwgx.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 模板列表响应DTO（用于前端选择模板）
 */
@Data
@Schema(description = "模板列表项，用于前端选择模板")
public class TemplateListResponse {

    @Schema(description = "模板ID（该模板下第一个工序模板的ID，用于创建循环时传递）", example = "1")
    private Long templateId;

    @Schema(description = "模板名称（用于显示）", example = "标准模板")
    private String templateName;
    
    @Schema(description = "控制时长标准（分钟），该模板下所有工序的控制时间总和", example = "1200")
    private Integer controlDuration;
}

