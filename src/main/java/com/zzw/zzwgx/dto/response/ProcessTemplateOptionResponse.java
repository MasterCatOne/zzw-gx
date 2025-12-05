package com.zzw.zzwgx.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 工序模板选择项响应DTO（用于前端下拉选择）
 */
@Data
@Schema(description = "工序模板选择项，用于前端选择工序名称")
public class ProcessTemplateOptionResponse {

    @Schema(description = "模板ID（选择后用于创建工序时传递templateId）", example = "1")
    private Long templateId;

    @Schema(description = "工序名称（用于显示在下拉框中）", example = "测量放样")
    private String processName;

    @Schema(description = "模板名称（可选，用于区分不同模板中的同名工序）", example = "标准模板")
    private String templateName;
}

