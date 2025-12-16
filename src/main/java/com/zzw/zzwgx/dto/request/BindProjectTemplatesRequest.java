package com.zzw.zzwgx.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.zzw.zzwgx.dto.deserializer.SingleOrListDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 按工点绑定模板 请求DTO
 */
@Data
@Schema(description = "按工点绑定模板请求参数")
public class BindProjectTemplatesRequest {

    @Schema(description = "模板ID列表。可传单个值如 1，也可传数组如 [1,2]", example = "[1,2]")
    @NotNull(message = "模板ID列表不能为空")
    @JsonDeserialize(using = SingleOrListDeserializer.class)
    private List<Long> templateIds;
}


