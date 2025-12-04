package com.zzw.zzwgx.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzw.zzwgx.common.Result;
import com.zzw.zzwgx.dto.request.CreateCycleRequest;
import com.zzw.zzwgx.dto.request.CreateProcessRequest;
import com.zzw.zzwgx.dto.request.UpdateProcessRequest;
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
import io.swagger.v3.oas.annotations.Parameter;
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
    
    @Operation(summary = "获取工点列表", description = "分页查询工点列表，支持按名称和状态搜索。响应数据包含工点ID、名称、状态、当前循环次数、围岩等级等信息。当前阶段为了方便前端联调，可选传入用户ID进行权限过滤；正式环境建议通过登录token自动识别用户。")
    @GetMapping("/projects")
    public Result<Page<ProjectListResponse>> getProjects(
            @Parameter(description = "页码，从1开始", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页记录数", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "工点名称关键词，支持模糊搜索", example = "工点1") @RequestParam(required = false) String name,
            @Parameter(description = "工点状态：IN_PROGRESS/COMPLETED/PAUSED", example = "IN_PROGRESS") @RequestParam(required = false) String status,
            @Parameter(description = "用户ID（测试/联调用，可选）。如果不传，则根据当前登录用户的token进行权限控制；如果传入，则按该用户的工点权限进行过滤。", example = "2") @RequestParam(required = false) Long userId) {
        log.info("查询工点列表，页码: {}, 每页大小: {}, 名称关键词: {}, 状态: {}, 指定用户ID: {}", pageNum, pageSize, name, status, userId);
        Page<ProjectListResponse> response = projectService.getProjectList(pageNum, pageSize, name, status, userId);
        return Result.success(response);
    }
    
    @Operation(summary = "获取工点进度详情", description = "获取指定工点指定循环的进度详情，包括循环信息、控制总时间、上循环结束时间、本循环开始时间、当前工序和工序列表等详细信息。如果不指定循环号，则返回最新循环的进度详情。")
    @GetMapping("/projects/{projectId}/progress")
    public Result<ProgressDetailResponse> getProgressDetail(
            @Parameter(description = "工点项目ID", required = true, example = "1") @PathVariable Long projectId,
            @Parameter(description = "循环号，不指定则返回最新循环", example = "2") @RequestParam(required = false) Integer cycleNumber) {
        log.info("查询工点进度详情，项目ID: {}, 循环号: {}", projectId, cycleNumber);
        ProgressDetailResponse response = projectService.getProgressDetail(projectId, cycleNumber);
        return Result.success(response);
    }
    
    @Operation(summary = "新建循环", description = "为指定工点创建新循环。预估开始时间会自动设置为与实际开始时间一致，预计结束时间会根据实际开始时间和控制时长自动计算。")
    @PostMapping("/cycles")
    public Result<CycleResponse> createCycle(@Valid @RequestBody CreateCycleRequest request) {
        log.info("创建新循环，项目ID: {}, 模板ID: {}, 控制时长: {}分钟", 
                request.getProjectId(), request.getTemplateId(), request.getControlDuration());
        CycleResponse response = cycleService.createCycle(request);
        return Result.success(response);
    }
    
    @Operation(summary = "获取工点循环列表", description = "分页查询指定工点下的所有循环记录，返回循环的基本信息，包括循环号、状态、时间、进尺、围岩等级等。")
    @GetMapping("/projects/{projectId}/cycles")
    public Result<Page<CycleResponse>> getProjectCycles(
            @Parameter(description = "工点项目ID", required = true, example = "1") @PathVariable Long projectId,
            @Parameter(description = "页码，从1开始", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页记录数", example = "10") @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<CycleResponse> page = cycleService.getCyclesByProject(projectId, pageNum, pageSize);
        return Result.success(page);
    }
    
    @Operation(summary = "获取循环详情", description = "查询单个循环的详细信息，包括循环号、控制时长、开始结束时间、进尺、围岩等级等。")
    @GetMapping("/cycles/{cycleId}")
    public Result<CycleResponse> getCycleDetail(
            @Parameter(description = "循环ID", required = true, example = "1") @PathVariable Long cycleId) {
        CycleResponse response = cycleService.getCycleDetail(cycleId);
        return Result.success(response);
    }
    
    @Operation(summary = "更新循环信息", description = "修改循环的控制时长、开始结束时间、状态、进尺、围岩等级等信息。如果更新为进行中状态，会检查该工点是否已有其他进行中的循环。")
    @PutMapping("/cycles/{cycleId}")
    public Result<CycleResponse> updateCycle(
            @Parameter(description = "循环ID", required = true, example = "1") @PathVariable Long cycleId,
            @Valid @RequestBody UpdateCycleRequest request) {
        CycleResponse response = cycleService.updateCycle(cycleId, request);
        return Result.success(response);
    }
    
    @Operation(summary = "新建工序", description = "为指定循环创建新工序。需要提供循环ID、工序名称、控制时间、施工人员ID等信息。")
    @PostMapping("/processes")
    public Result<ProcessResponse> createProcess(@Valid @RequestBody CreateProcessRequest request) {
        log.info("创建新工序，循环ID: {}, 工序名称: {}, 施工人员ID: {}", 
                request.getCycleId(), request.getName(), request.getWorkerId());
        ProcessResponse response = processService.createProcess(request);
        return Result.success(response);
    }
    
    @Operation(summary = "获取工序详情", description = "获取指定工序的详细信息，包括工序名称、操作员、状态、控制时间、实际时间、超时/节时情况等。")
    @GetMapping("/processes/{processId}")
    public Result<ProcessDetailResponse> getProcessDetail(
            @Parameter(description = "工序ID", required = true, example = "1") @PathVariable Long processId) {
        log.info("查询工序详情，工序ID: {}", processId);
        ProcessDetailResponse response = processService.getProcessDetail(processId);
        return Result.success(response);
    }

    @Operation(summary = "更新工序", description = "更新指定工序的信息，包括工序名称、控制时间、状态、开始结束时间、操作员、进尺等字段，支持部分字段更新。")
    @PutMapping("/processes/{processId}")
    public Result<ProcessResponse> updateProcess(
            @Parameter(description = "工序ID", required = true, example = "1") @PathVariable Long processId,
            @Valid @RequestBody UpdateProcessRequest request) {
        log.info("更新工序，工序ID: {}", processId);
        ProcessResponse response = processService.updateProcess(processId, request);
        return Result.success(response);
    }
}

