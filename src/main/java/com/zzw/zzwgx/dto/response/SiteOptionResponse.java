package com.zzw.zzwgx.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "工点下拉选项")
public class SiteOptionResponse {
    
    @Schema(description = "工点ID", example = "123")
    private Long id;
    
    @Schema(description = "工点名称", example = "上行隧道入口工点")
    private String name;
}

