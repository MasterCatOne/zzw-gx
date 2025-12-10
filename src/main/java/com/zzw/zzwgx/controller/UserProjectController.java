package com.zzw.zzwgx.controller;

import com.zzw.zzwgx.common.Result;
import com.zzw.zzwgx.dto.request.UserProjectAssignRequest;
import com.zzw.zzwgx.service.UserProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户工点分配控制器
 */
@Slf4j
@Tag(name = "用户工点分配", description = "用户与工点关联管理接口")
@RestController
@RequestMapping("/api/admin/user-projects")
@RequiredArgsConstructor
public class UserProjectController {
    
    private final UserProjectService userProjectService;
    
    @Operation(summary = "查询用户管理的工点", description = "根据用户用户ID查询其可管理的工点列表。返回该用户被分配的所有工点ID列表。如果用户被分配到父节点（如标段、隧道），则自动包含其下所有子工点。")
    @GetMapping("/{userId}")
    public Result<List<Long>> getUserProjects(
            @Parameter(description = "用户用户ID", required = true, example = "2") @PathVariable Long userId) {
        List<Long> projectIds = userProjectService.getProjectIdsByUser(userId);
        return Result.success(projectIds);
    }
    
    @Operation(summary = "分配用户工点", description = "为用户分配可管理的工点列表。可以分配工点ID列表，如果分配父节点（如标段、隧道），则该用户可以查看该节点下所有子工点。传空列表表示清空该用户的所有工点权限。")
    @PostMapping("/assign")
    public Result<Void> assignProjects(@Valid @RequestBody UserProjectAssignRequest request) {
        userProjectService.assignProjects(request.getUserId(), request.getProjectIds());
        return Result.success();
    }
}

