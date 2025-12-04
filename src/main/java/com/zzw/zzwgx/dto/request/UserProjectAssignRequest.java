package com.zzw.zzwgx.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 管理员工点分配请求
 */
@Data
@Schema(description = "管理员项目分配请求参数")
public class UserProjectAssignRequest {
    
    @Schema(description = "管理员用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @Schema(description = "分配的项目节点ID列表，可为空表示清空权限", example = "[1, 2, 3]")
    private List<Long> projectIds;
}

