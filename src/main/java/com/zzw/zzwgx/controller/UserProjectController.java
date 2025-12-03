package com.zzw.zzwgx.controller;

import com.zzw.zzwgx.common.Result;
import com.zzw.zzwgx.dto.request.UserProjectAssignRequest;
import com.zzw.zzwgx.service.UserProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员工点分配控制器
 */
@Slf4j
@Tag(name = "管理员工点分配", description = "管理员与工点关联管理接口")
@RestController
@RequestMapping("/api/admin/user-projects")
@RequiredArgsConstructor
public class UserProjectController {
    
    private final UserProjectService userProjectService;
    
    @Operation(summary = "查询管理员管理的工点", description = "根据管理员用户ID查询其可管理的工点列表")
    @GetMapping("/{userId}")
    public Result<List<Long>> getUserProjects(@PathVariable Long userId) {
        List<Long> projectIds = userProjectService.getProjectIdsByUser(userId);
        return Result.success(projectIds);
    }
    
    @Operation(summary = "分配管理员工点", description = "为管理员分配可管理的工点列表，传空表示清空权限")
    @PostMapping("/assign")
    public Result<Void> assignProjects(@Valid @RequestBody UserProjectAssignRequest request) {
        userProjectService.assignProjects(request.getUserId(), request.getProjectIds());
        return Result.success();
    }
}

