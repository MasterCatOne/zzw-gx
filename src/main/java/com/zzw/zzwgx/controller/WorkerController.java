package com.zzw.zzwgx.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzw.zzwgx.common.Result;
import com.zzw.zzwgx.dto.request.FillProcessTimeRequest;
import com.zzw.zzwgx.dto.request.WorkerStartProcessRequest;
import com.zzw.zzwgx.dto.request.WorkerUpdateProfileRequest;
import com.zzw.zzwgx.dto.response.*;
import com.zzw.zzwgx.security.SecurityUtils;
import com.zzw.zzwgx.service.StatisticsService;
import com.zzw.zzwgx.service.ProcessService;
import com.zzw.zzwgx.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    private final StatisticsService statisticsService;
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

    @Operation(summary = "修改个人信息", description = "施工人员修改自己的姓名、手机号或密码。")
    @PutMapping("/profile")
    public Result<UserProfileResponse> updateMyProfile(@Valid @RequestBody WorkerUpdateProfileRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("施工人员修改个人信息，用户ID: {}", userId);
        UserProfileResponse response = userService.updateWorkerProfile(userId, request);
        return Result.success("修改成功", response);
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

    @Operation(summary = "完成工序", description = "施工人员点击完成工序，自动将实际结束时间置为当前时间并更新为已完成状态，同时计算节时/超时。")
    @PostMapping("/processes/{processId}/finish")
    public Result<ProcessDetailResponse> finishMyProcess(
            @Parameter(description = "工序ID", required = true, example = "1001") @PathVariable Long processId) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("施工人员完成工序，用户ID: {}, 工序ID: {}", userId, processId);
        processService.completeWorkerProcess(processId, userId);
//        ProcessDetailResponse response = processService.getWorkerProcessDetail(processId, userId);
        return Result.success("工序已完成", null);
    }

    @Operation(summary = "完成并进入下一循环", description = "用于已点过完成的工序，再提交超时原因后进入下一循环；若未超时则可直接提交。")
    @PostMapping("/processes/{processId}/finish-and-next")
    public Result<ProcessDetailResponse> finishAndNext(
            @Parameter(description = "工序ID", required = true, example = "1001") @PathVariable Long processId,
            @Parameter(description = "超时原因（仅超时必填）") @RequestParam(required = false) String overtimeReason) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("施工人员完成并进入下一循环，用户ID: {}, 工序ID: {}", userId, processId);

        // 这里需要检查循环状态，如果所有工序都完成则更新循环为已完成
        ProcessDetailResponse detail = processService.getWorkerProcessDetail(processId, userId);
        boolean isOvertime = detail.getTimeDifferenceText() != null && detail.getTimeDifferenceText().startsWith("超时");

        // 超时必须填写原因
        if (isOvertime && (overtimeReason == null || overtimeReason.isBlank())) {
            return Result.fail("超时工序需填写超时原因后才能进入下一循环");
        }
        if (isOvertime) {
            processService.submitOvertimeReason(processId, userId, overtimeReason);
//            detail = processService.getWorkerProcessDetail(processId, userId);
        }
        processService.completeWorkerProcessAndCheckCycle(processId, userId);
        // 未超时或已补充原因，视为可进入下一循环（此处仅返回提示，不创建新循环）
        return Result.success("已完成，可进入下一循环", null);
    }
    
    @Operation(summary = "补填工序时间", description = "施工人员补填工序的实际开始时间和实际结束时间。24小时内可直接补填，超过24小时（从预计结束时间开始计算）只能由系统管理员补填。")
    @PostMapping("/processes/{processId}/fill-time")
    public Result<ProcessResponse> fillProcessTime(
            @Parameter(description = "工序ID", required = true, example = "1001") @PathVariable Long processId,
            @Valid @RequestBody FillProcessTimeRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("施工人员补填工序时间，用户ID: {}, 工序ID: {}, 实际开始时间: {}, 实际结束时间: {}", 
                userId, processId, request.getActualStartTime(), request.getActualEndTime());
        ProcessResponse response = processService.fillProcessTime(processId, userId, request);
        return Result.success("时间补填成功", response);
    }
    
    @Operation(summary = "我的工点本周超耗统计", description = "按工点汇总当前施工人员本周完成工序的超时/节时总计（单位：小时）。")
    @GetMapping("/statistics/overtime-week")
    public Result<List<StatisticsResponse.OvertimeStat>> getMyOvertimeStatistics() {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("施工人员查询本周超耗统计，用户ID: {}", userId);
        List<StatisticsResponse.OvertimeStat> stats =
                statisticsService.getWorkerOvertimeStatistics(userId);
        return Result.success(stats);
    }

}

