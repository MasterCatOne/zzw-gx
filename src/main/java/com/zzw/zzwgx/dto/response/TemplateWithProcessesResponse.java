package com.zzw.zzwgx.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 模板及其工序列表响应DTO
 */
@Data
@Schema(description = "模板及其工序列表响应")
public class TemplateWithProcessesResponse {
    
    @Schema(description = "模板名称", example = "标准模板")
    private String templateName;
    
    @Schema(description = "模板ID（该模板下第一个工序模板的ID）", example = "1")
    private Long templateId;
    
    @Schema(description = "该模板下的所有工序列表，按默认顺序排序")
    private List<ProcessTemplateResponse> processes;
}

