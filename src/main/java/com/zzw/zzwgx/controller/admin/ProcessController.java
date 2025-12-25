package com.zzw.zzwgx.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzw.zzwgx.common.Result;
import com.zzw.zzwgx.dto.request.CreateProcessRequest;
import com.zzw.zzwgx.dto.request.UpdateProcessOrderRequest;
import com.zzw.zzwgx.dto.request.UpdateProcessRequest;
import com.zzw.zzwgx.dto.response.CycleProcessTimeResponse;
import com.zzw.zzwgx.dto.response.OvertimeProcessResponse;
import com.zzw.zzwgx.dto.response.ProcessDetailResponse;
import com.zzw.zzwgx.dto.response.ProcessResponse;
import com.zzw.zzwgx.service.ProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 工序管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class ProcessController {
    
    private final ProcessService processService;
    
    @Operation(summary = "新建并开工工序", description = "为指定循环创建工序且状态直接为进行中，需填写实际开始时间；会根据实际/预计开始时间与控制时长计算预计结束时间。", tags = {"管理员管理-工序管理"})
    @PostMapping("/processes/start-now")
    public Result<ProcessResponse> createProcessAndStart(@Valid @RequestBody CreateProcessRequest request) {
        log.info("创建并开工工序，循环ID: {}, 工序字典ID: {}, 施工人员ID: {}, 控制时长: {}, 实际开始时间: {}",
                request.getCycleId(), request.getProcessCatalogId(), request.getWorkerId(), request.getControlTime(), request.getActualStartTime());
        ProcessResponse response = processService.createProcessAndStart(request);
        return Result.success(response);
    }
    
    @Operation(summary = "获取工序详情", description = "获取指定工序的详细信息，包括工序名称、操作员、状态、控制时间、实际时间、超时/节时情况等。", tags = {"管理员管理-工序管理"})
    @GetMapping("/processes/{processId}")
    public Result<ProcessDetailResponse> getProcessDetail(
            @Parameter(description = "工序ID", required = true, example = "1") @PathVariable Long processId) {
        log.info("查询工序详情，工序ID: {}", processId);
        ProcessDetailResponse response = processService.getProcessDetail(processId);
        return Result.success(response);
    }

    @Operation(summary = "更新工序", description = "更新指定工序的信息，包括工序名称、控制时间、状态、开始结束时间、操作员、进尺等字段，支持部分字段更新。注意：各工序控制时间可调整，仅限管理员操作。", tags = {"管理员管理-工序管理"})
    @PutMapping("/processes/{processId}")
    public Result<ProcessResponse> updateProcess(
            @Parameter(description = "工序ID", required = true, example = "1") @PathVariable Long processId,
            @Valid @RequestBody UpdateProcessRequest request) {
        log.info("更新工序，工序ID: {}, 控制时间: {}", processId, request.getControlTime());
        ProcessResponse response = processService.updateProcess(processId, request);
        return Result.success(response);
    }
    
    @Operation(summary = "批量更新工序顺序", description = "批量更新指定循环下工序的执行顺序，支持工序顺序的灵活调整。", tags = {"管理员管理-工序管理"})
    @PutMapping("/cycles/{cycleId}/processes/orders")
    public Result<Void> updateProcessOrders(
            @Parameter(description = "循环ID", required = true, example = "1") @PathVariable Long cycleId,
            @Valid @RequestBody UpdateProcessOrderRequest request) {
        log.info("批量更新工序顺序，循环ID: {}", cycleId);
        processService.updateProcessOrders(cycleId, request);
        return Result.success();
    }
    
    @Operation(summary = "计算循环工序总时间", description = "计算指定循环的工序总时间统计。返回单工序总时间（所有工序实际完成时间的总和）和整套工序总时间（考虑重叠时间不重复计算）。单工序时间依旧按照实际完成时间进行统计。", tags = {"管理员管理-工序管理"})
    @GetMapping("/cycles/{cycleId}/process-time")
    public Result<CycleProcessTimeResponse> calculateCycleProcessTime(
            @Parameter(description = "循环ID", required = true, example = "1") @PathVariable Long cycleId) {
        log.info("计算循环工序总时间，循环ID: {}", cycleId);
        CycleProcessTimeResponse response = processService.calculateCycleProcessTime(cycleId);
        return Result.success(response);
    }
    
    @Operation(summary = "查询超时未填报原因的工序列表", description = "查询所有超时但未填报超时原因的工序列表，仅返回循环未完成的工序。用于管理员督促施工人员填报超时原因。返回信息包括工点名称、工序信息、超时时间等。", tags = {"管理员管理-工序管理"})
    @GetMapping("/processes/overtime-without-reason")
    public Result<Page<OvertimeProcessResponse>> getOvertimeProcessesWithoutReason(
            @Parameter(description = "页码，从1开始", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页记录数", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "工点名称关键词，支持模糊搜索", example = "工点1") @RequestParam(required = false) String projectName) {
        log.info("查询超时未填报原因的工序列表，页码: {}, 大小: {}, 工点名称: {}", pageNum, pageSize, projectName);
        Page<OvertimeProcessResponse> page = processService.getOvertimeProcessesWithoutReason(pageNum, pageSize, projectName);
        return Result.success(page);
    }
    
    // 授权接口已废弃，超过24小时的工序只能由系统管理员直接补填，无需授权步骤
    // @Operation(summary = "授权工序时间补填", description = "系统管理员授权指定工序的时间补填。超过24小时的工序补填需要系统管理员授权后才能进行。", tags = {"管理员管理-工序管理"})
    // @PostMapping("/processes/{processId}/authorize-time-fill")
    // public Result<Void> authorizeTimeFill(
    //         @Parameter(description = "工序ID", required = true, example = "1") @PathVariable Long processId) {
    //     log.info("系统管理员授权工序时间补填，工序ID: {}", processId);
    //     processService.authorizeTimeFill(processId);
    //     return Result.success();
    // }
}

