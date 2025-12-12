package com.zzw.zzwgx.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "隧道下拉选项")
public class TunnelOptionResponse {
    
    @Schema(description = "隧道ID", example = "123")
    private Long id;
    
    @Schema(description = "隧道名称", example = "1标段上行隧道")
    private String name;
}

