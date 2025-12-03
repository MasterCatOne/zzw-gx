package com.zzw.zzwgx.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzw.zzwgx.common.Result;
import com.zzw.zzwgx.dto.request.CreateCycleRequest;
import com.zzw.zzwgx.dto.request.CreateProcessRequest;
import com.zzw.zzwgx.dto.request.UpdateCycleRequest;
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
    
    @Operation(summary = "获取工点列表", description = "分页查询工点列表，支持按名称和状态搜索")
    @GetMapping("/projects")
    public Result<Page<ProjectListResponse>> getProjects(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status) {
        log.info("查询工点列表，页码: {}, 每页大小: {}, 名称关键词: {}, 状态: {}", pageNum, pageSize, name, status);
        Page<ProjectListResponse> response = projectService.getProjectList(pageNum, pageSize, name, status);
        return Result.success(response);
    }
    
    @Operation(summary = "获取项目进度详情", description = "获取指定项目指定循环的进度详情，包括循环信息和工序列表")
    @GetMapping("/projects/{projectId}/progress")
    public Result<ProgressDetailResponse> getProgressDetail(@PathVariable Long projectId,
                                                            @RequestParam(required = false) Integer cycleNumber) {
        log.info("查询项目进度详情，项目ID: {}, 循环号: {}", projectId, cycleNumber);
        ProgressDetailResponse response = projectService.getProgressDetail(projectId, cycleNumber);
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
    
    @Operation(summary = "获取项目循环列表", description = "分页查询指定项目下的所有循环记录")
    @GetMapping("/projects/{projectId}/cycles")
    public Result<Page<CycleResponse>> getProjectCycles(@PathVariable Long projectId,
                                                        @RequestParam(defaultValue = "1") Integer pageNum,
                                                        @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<CycleResponse> page = cycleService.getCyclesByProject(projectId, pageNum, pageSize);
        return Result.success(page);
    }
    
    @Operation(summary = "获取循环详情", description = "查询单个循环的详细信息")
    @GetMapping("/cycles/{cycleId}")
    public Result<CycleResponse> getCycleDetail(@PathVariable Long cycleId) {
        CycleResponse response = cycleService.getCycleDetail(cycleId);
        return Result.success(response);
    }
    
    @Operation(summary = "更新循环信息", description = "修改循环的控制时长、时间、状态等信息")
    @PutMapping("/cycles/{cycleId}")
    public Result<CycleResponse> updateCycle(@PathVariable Long cycleId,
                                             @Valid @RequestBody UpdateCycleRequest request) {
        CycleResponse response = cycleService.updateCycle(cycleId, request);
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

