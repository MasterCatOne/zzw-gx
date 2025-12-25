package com.zzw.zzwgx.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 当前循环进行中工序顺序响应DTO
 */
@Data
@Schema(description = "当前循环进行中工序顺序响应")
public class InProgressProcessOrderResponse {
    
    @Schema(description = "进行中工序的开始顺序", example = "1")
    private Integer startOrder;
}

