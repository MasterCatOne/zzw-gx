package com.zzw.zzwgx.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.zzw.zzwgx.dto.deserializer.SingleOrListDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 绑定模板到工点 请求DTO
 */
@Data
@Schema(description = "绑定模板到工点请求参数")
public class BindTemplateProjectsRequest {

    @Schema(description = "工点ID列表（仅 SITE）。可传单个值如 1，也可传数组如 [1,2]", example = "[1,2]")
    @NotNull(message = "工点ID列表不能为空")
    @JsonDeserialize(using = SingleOrListDeserializer.class)
    private List<Long> projectIds;
}


