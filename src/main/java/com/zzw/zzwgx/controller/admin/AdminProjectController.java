package com.zzw.zzwgx.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzw.zzwgx.common.Result;
import com.zzw.zzwgx.dto.request.ProjectRequest;
import com.zzw.zzwgx.dto.response.ProgressDetailResponse;
import com.zzw.zzwgx.dto.response.ProjectListResponse;
import com.zzw.zzwgx.dto.response.ProjectTreeNodeResponse;
import com.zzw.zzwgx.dto.response.SiteConstructionStatusResponse;
import com.zzw.zzwgx.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 工点管理控制器（管理员）
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminProjectController {
    
    private final ProjectService projectService;
    
    @Operation(summary = "获取工点列表", description = "分页查询工点列表，支持按名称和状态搜索。响应数据包含工点ID、名称、状态、当前循环次数、围岩等级等信息。当前阶段为了方便前端联调，可选传入用户ID进行权限过滤；正式环境建议通过登录token自动识别用户。", tags = {"管理员管理-工点管理"})
    @GetMapping("/projects")
    public Result<Page<ProjectListResponse>> getProjects(
            @Parameter(description = "页码，从1开始", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页记录数", example = "10") @RequestParam(defaultValue = "1000") Integer pageSize,
            @Parameter(description = "工点名称关键词，支持模糊搜索", example = "工点1") @RequestParam(required = false) String name,
            @Parameter(description = "工点状态：IN_PROGRESS/COMPLETED/PAUSED", example = "IN_PROGRESS") @RequestParam(required = false) String status,
            @Parameter(description = "用户ID（测试/联调用，可选）。如果不传，则根据当前登录用户的token进行权限控制；如果传入，则按该用户的工点权限进行过滤。", example = "2") @RequestParam(required = false) Long userId) {
        log.info("查询工点列表，页码: {}, 每页大小: {}, 名称关键词: {}, 状态: {}, 指定用户ID: {}", pageNum, pageSize, name, status, userId);
        Page<ProjectListResponse> response = projectService.getProjectList(pageNum, pageSize, name, status, userId);
        return Result.success(response);
    }
    
    @Operation(summary = "创建工点信息", description = "管理员创建工点信息。需要指定父节点ID（如果为顶级节点则传null）、节点类型（PROJECT/SECTION/TUNNEL/SITE）、节点名称、编号等信息。", tags = {"管理员管理-工点管理"})
    @PostMapping("/projects")
    public Result<ProjectTreeNodeResponse> createProject(@Valid @RequestBody ProjectRequest request) {
        log.info("管理员创建工点信息，节点类型: {}, 节点名称: {}", request.getNodeType(), request.getProjectName());
        ProjectTreeNodeResponse node = projectService.createProject(request);
        return Result.success(node);
    }
    
    @Operation(summary = "修改工点信息", description = "管理员修改工点信息。可以修改节点名称、编号、描述、状态等，但不能修改节点类型和父节点。", tags = {"管理员管理-工点管理"})
    @PutMapping("/projects/{projectId}")
    public Result<ProjectTreeNodeResponse> updateProject(
            @Parameter(description = "项目节点ID", required = true, example = "1") @PathVariable Long projectId,
            @Valid @RequestBody ProjectRequest request) {
        log.info("管理员修改工点信息，项目ID: {}", projectId);
        ProjectTreeNodeResponse node = projectService.updateProject(projectId, request);
        return Result.success(node);
    }
    
    @Operation(summary = "获取工点进度详情", description = "获取指定工点指定循环的进度详情，包括循环信息、控制总时间、上循环结束时间、本循环开始时间、当前工序和工序列表等详细信息。如果不指定循环号，则返回最新循环的进度详情。", tags = {"管理员管理-工点管理"})
    @GetMapping("/projects/{projectId}/progress")
    public Result<ProgressDetailResponse> getProgressDetail(
            @Parameter(description = "工点项目ID", required = true, example = "1") @PathVariable Long projectId,
            @Parameter(description = "循环号，不指定则返回最新循环", example = "2") @RequestParam(required = false) Integer cycleNumber) {
        log.info("查询工点进度详情，项目ID: {}, 循环号: {}", projectId, cycleNumber);
        ProgressDetailResponse response = projectService.getProgressDetail(projectId, cycleNumber);
        return Result.success(response);
    }
    
    @Operation(summary = "查看工点施工状态", description = "管理员查看各工点的当前施工状态，包括当前工序持续时长、上几道工序的完成情况和节超情况。返回当前正在进行的工序信息和已完成的工序列表。", tags = {"管理员管理-工点管理"})
    @GetMapping("/projects/{projectId}/construction-status")
    public Result<SiteConstructionStatusResponse> getSiteConstructionStatus(
            @Parameter(description = "工点项目ID", required = true, example = "1") @PathVariable Long projectId) {
        log.info("管理员查看工点施工状态，项目ID: {}", projectId);
        SiteConstructionStatusResponse response = projectService.getSiteConstructionStatus(projectId);
        return Result.success(response);
    }
}

