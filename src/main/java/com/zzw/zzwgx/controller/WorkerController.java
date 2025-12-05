package com.zzw.zzwgx.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzw.zzwgx.common.Result;
import com.zzw.zzwgx.dto.request.SubmitOvertimeReasonRequest;
import com.zzw.zzwgx.dto.request.WorkerStartProcessRequest;
import com.zzw.zzwgx.dto.response.ProcessDetailResponse;
import com.zzw.zzwgx.dto.response.StartProcessResponse;
import com.zzw.zzwgx.dto.response.UserProfileResponse;
import com.zzw.zzwgx.dto.response.WorkerProcessListResponse;
import com.zzw.zzwgx.security.SecurityUtils;
import com.zzw.zzwgx.service.ProcessService;
import com.zzw.zzwgx.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 施工人员控制器
 */
@Slf4j
@Tag(name = "施工人员管理", description = "施工人员相关接口")
@RestController
@RequestMapping("/api/worker")
@RequiredArgsConstructor
public class WorkerController {

    private final UserService userService;
    private final ProcessService processService;
    @Operation(summary = "获取个人信息", description = "获取当前登录用户的个人信息，包括用户ID、用户名、真实姓名、角色列表、身份证号、手机号等。")
    @GetMapping("/profile")
    public Result<UserProfileResponse> getProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("查询个人信息，用户ID: {}", userId);
        UserProfileResponse response = userService.getProfile(userId);
        return Result.success(response);
    }

    @Operation(summary = "获取我的工序列表", description = "施工人员查看自己的工序任务列表，支持按工点名称和工序状态筛选。返回工点名称、工序任务、工序状态、当前循环、任务时间等信息。")
    @GetMapping("/processes")
    public Result<Page<WorkerProcessListResponse>> getMyProcesses(
            @Parameter(description = "页码，从1开始", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页记录数", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "工点名称关键词，支持模糊搜索", example = "工点1") @RequestParam(required = false) String projectName,
            @Parameter(description = "工序状态：NOT_STARTED/IN_PROGRESS/COMPLETED", example = "IN_PROGRESS") @RequestParam(required = false) String status) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("施工人员查询自己的工序列表，用户ID: {}, 页码: {}, 大小: {}, 工点名称: {}, 状态: {}",
                userId, pageNum, pageSize, projectName, status);
        Page<WorkerProcessListResponse> page = processService.getWorkerProcessList(userId, pageNum, pageSize, projectName, status);
        return Result.success(page);
    }

    @Operation(summary = "获取我的工序详情", description = "施工人员查看指定工序的详细信息，包括工序名称、状态、耗时、循环号、上一工序状态、开始结束时间、超时原因等。")
    @GetMapping("/processes/{processId}")
    public Result<ProcessDetailResponse> getMyProcessDetail(
            @Parameter(description = "工序ID", required = true, example = "1001") @PathVariable Long processId) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("施工人员查询工序详情，用户ID: {}, 工序ID: {}", userId, processId);
        ProcessDetailResponse response = processService.getWorkerProcessDetail(processId, userId);
        return Result.success(response);
    }

    @Operation(summary = "开始工序", description = "施工人员立即开始工序任务，需要选择实际开始时间。接口将工序状态更新为进行中。如果上一工序未完成，会返回提示信息，但不阻止开始。")
    @PostMapping("/processes/{processId}/start")
    public Result<StartProcessResponse> startMyProcess(
            @Parameter(description = "工序ID", required = true, example = "1001") @PathVariable Long processId,
            @Valid @RequestBody WorkerStartProcessRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("施工人员开始工序，用户ID: {}, 工序ID: {}", userId, processId);
        StartProcessResponse response = processService.startWorkerProcess(processId, userId, request.getActualStartTime());
        return Result.success(response);
    }

    @Operation(summary = "填报超时原因", description = "施工人员填报工序超时原因。仅限已完成的超时工序，且只能在循环完成前填报。")
    @PostMapping("/processes/{processId}/overtime-reason")
    public Result<Void> submitOvertimeReason(
            @Parameter(description = "工序ID", required = true, example = "1001") @PathVariable Long processId,
            @Valid @RequestBody SubmitOvertimeReasonRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("施工人员填报超时原因，用户ID: {}, 工序ID: {}", userId, processId);
        processService.submitOvertimeReason(processId, userId, request.getOvertimeReason());
        return Result.success("超时原因填报成功", null);
    }
}

