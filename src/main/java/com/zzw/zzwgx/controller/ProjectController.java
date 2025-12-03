package com.zzw.zzwgx.controller;

import com.zzw.zzwgx.common.Result;
import com.zzw.zzwgx.dto.request.ProjectRequest;
import com.zzw.zzwgx.dto.response.ProjectTreeNodeResponse;
import com.zzw.zzwgx.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 项目树控制器
 */
@Slf4j
@Tag(name = "项目树管理", description = "项目/标段/隧道/工点树结构接口")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {
    
    private final ProjectService projectService;
    
    @Operation(summary = "获取项目树", description = "获取项目-标段-隧道-工点等完整树结构")
    @GetMapping("/tree")
    public Result<List<ProjectTreeNodeResponse>> getProjectTree() {
        List<ProjectTreeNodeResponse> tree = projectService.getProjectTree();
        return Result.success(tree);
    }
    
    @Operation(summary = "新增项目节点", description = "创建项目树中的节点")
    @PostMapping
    public Result<ProjectTreeNodeResponse> createProject(@Valid @RequestBody ProjectRequest request) {
        ProjectTreeNodeResponse node = projectService.createProject(request);
        return Result.success(node);
    }
    
    @Operation(summary = "编辑项目节点", description = "更新项目树中的节点")
    @PutMapping("/{projectId}")
    public Result<ProjectTreeNodeResponse> updateProject(@PathVariable Long projectId,
                                                         @Valid @RequestBody ProjectRequest request) {
        ProjectTreeNodeResponse node = projectService.updateProject(projectId, request);
        return Result.success(node);
    }
    
    @Operation(summary = "删除项目节点", description = "删除项目树中的节点，删除前需清理子节点")
    @DeleteMapping("/{projectId}")
    public Result<Void> deleteProject(@PathVariable Long projectId) {
        projectService.deleteProject(projectId);
        return Result.success();
    }
}

