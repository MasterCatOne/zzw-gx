package com.zzw.zzwgx.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzw.zzwgx.common.Result;
import com.zzw.zzwgx.dto.request.CreateCycleRequest;
import com.zzw.zzwgx.dto.request.CreateProcessRequest;
import com.zzw.zzwgx.dto.response.CycleResponse;
import com.zzw.zzwgx.dto.response.ProcessDetailResponse;
import com.zzw.zzwgx.dto.response.ProcessResponse;
import com.zzw.zzwgx.dto.response.ProgressDetailResponse;
import com.zzw.zzwgx.dto.response.ProjectListResponse;
import com.zzw.zzwgx.service.CycleService;
import com.zzw.zzwgx.service.ProcessService;
import com.zzw.zzwgx.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员控制器
 */
@Slf4j
@Tag(name = "管理员管理", description = "管理员相关接口")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final ProjectService projectService;
    private final CycleService cycleService;
    private final ProcessService processService;
    
    @Operation(summary = "获取项目列表", description = "分页查询项目列表，支持按工点名称搜索")
    @GetMapping("/projects")
    public Result<Page<ProjectListResponse>> getProjects(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name) {
        log.info("查询项目列表，页码: {}, 每页大小: {}, 搜索关键词: {}", pageNum, pageSize, name);
        Page<ProjectListResponse> response = projectService.getProjectList(pageNum, pageSize, name);
        return Result.success(response);
    }
    
    @Operation(summary = "获取项目进度详情", description = "获取指定项目的进度详情，包括循环信息和工序列表")
    @GetMapping("/projects/{projectId}/progress")
    public Result<ProgressDetailResponse> getProgressDetail(@PathVariable Long projectId) {
        log.info("查询项目进度详情，项目ID: {}", projectId);
        ProgressDetailResponse response = projectService.getProgressDetail(projectId);
        return Result.success(response);
    }
    
    @Operation(summary = "新建循环", description = "为指定项目创建新循环")
    @PostMapping("/cycles")
    public Result<CycleResponse> createCycle(@Valid @RequestBody CreateCycleRequest request) {
        log.info("创建新循环，项目ID: {}, 模板ID: {}, 控制时长: {}分钟", 
                request.getProjectId(), request.getTemplateId(), request.getControlDuration());
        CycleResponse response = cycleService.createCycle(request);
        return Result.success(response);
    }
    
    @Operation(summary = "新建工序", description = "为指定循环创建新工序")
    @PostMapping("/processes")
    public Result<ProcessResponse> createProcess(@Valid @RequestBody CreateProcessRequest request) {
        log.info("创建新工序，循环ID: {}, 工序名称: {}, 施工人员ID: {}", 
                request.getCycleId(), request.getName(), request.getWorkerId());
        ProcessResponse response = processService.createProcess(request);
        return Result.success(response);
    }
    
    @Operation(summary = "获取工序详情", description = "获取指定工序的详细信息")
    @GetMapping("/processes/{processId}")
    public Result<ProcessDetailResponse> getProcessDetail(@PathVariable Long processId) {
        log.info("查询工序详情，工序ID: {}", processId);
        ProcessDetailResponse response = processService.getProcessDetail(processId);
        return Result.success(response);
    }
}

