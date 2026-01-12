package com.zzw.zzwgx.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzw.zzwgx.common.Result;
import com.zzw.zzwgx.dto.request.CreateCycleRequest;
import com.zzw.zzwgx.dto.request.UpdateCycleRequest;
import com.zzw.zzwgx.dto.response.CycleReportDataResponse;
import com.zzw.zzwgx.dto.response.CycleResponse;
import com.zzw.zzwgx.dto.response.InProgressProcessOrderResponse;
import com.zzw.zzwgx.dto.response.TemplateControlDurationResponse;
import com.zzw.zzwgx.service.CycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 循环管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class CycleController {
    
    private final CycleService cycleService;
    
    @Operation(summary = "获取模板控制时长", description = "根据模板ID获取模板的控制时长（该模板下所有工序的控制时间总和）。用于创建循环页面，在用户选择模板后显示控制时长，无需创建循环即可预览。", tags = {"管理员管理-循环管理"})
    @GetMapping("/templates/{templateId}/control-duration")
    public Result<TemplateControlDurationResponse> getTemplateControlDuration(
            @Parameter(description = "模板ID（该模板下任意一个工序模板的ID）", required = true, example = "1") @PathVariable Long templateId) {
        log.info("获取模板控制时长，模板ID: {}", templateId);
        TemplateControlDurationResponse response = cycleService.getTemplateControlDuration(templateId);
        return Result.success(response);
    }
    
    @Operation(summary = "新建循环", description = "为指定工点创建新循环。预估开始时间会自动设置为与实际开始时间一致，预计结束时间会根据实际开始时间和控制时长自动计算。控制时长标准由系统根据所选模板中所有工序的控制时间总和自动计算（前端无需填写）。传入工序模板ID（该模板下任意一个工序模板的ID即可），后端会根据模板名称自动创建该模板下的所有工序。", tags = {"管理员管理-循环管理"})
    @PostMapping("/cycles")
    public Result<CycleResponse> createCycle(@Valid @RequestBody CreateCycleRequest request) {
        log.info("创建新循环，项目ID: {}, 模板ID: {}", 
                request.getProjectId(), request.getTemplateId());
        CycleResponse response = cycleService.createCycle(request);
        return Result.success(response);
    }
    
    @Operation(summary = "获取工点循环列表", description = "分页查询指定工点下的所有循环记录，返回循环的基本信息，包括循环号、状态、时间、进尺、围岩等级等。", tags = {"管理员管理-循环管理"})
    @GetMapping("/projects/{projectId}/cycles")
    public Result<Page<CycleResponse>> getProjectCycles(
            @Parameter(description = "工点项目ID", required = true, example = "1") @PathVariable Long projectId,
            @Parameter(description = "页码，从1开始", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页记录数", example = "10") @RequestParam(defaultValue = "1000") Integer pageSize) {
        Page<CycleResponse> page = cycleService.getCyclesByProject(projectId, pageNum, pageSize);
        return Result.success(page);
    }
    
    @Operation(summary = "获取循环详情", description = "查询单个循环的详细信息，包括循环号、控制时长、开始结束时间、进尺、围岩等级等。", tags = {"管理员管理-循环管理"})
    @GetMapping("/cycles/{cycleId}")
    public Result<CycleResponse> getCycleDetail(
            @Parameter(description = "循环ID", required = true, example = "1") @PathVariable Long cycleId) {
        CycleResponse response = cycleService.getCycleDetail(cycleId);
        return Result.success(response);
    }
    
    @Operation(summary = "导出循环报表", description = "基于Excel模板导出循环报表，生成并下载文件。", tags = {"管理员管理-循环管理"})
    @GetMapping("/cycles/{cycleId}/report")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN')")
    public void exportCycleReport(
            @Parameter(description = "循环ID", required = true, example = "1") @PathVariable Long cycleId,
            HttpServletResponse response) {
        log.info("导出循环报表，循环ID: {}", cycleId);
        cycleService.exportCycleReport(cycleId, response);
    }
    
    @Operation(summary = "获取循环报表数据", description = "获取循环报表中需要填写的单元格值，返回JSON格式数据，用于前端展示或手动填写Excel。", tags = {"管理员管理-循环管理"})
    @GetMapping("/cycles/{cycleId}/report-data")
    public Result<CycleReportDataResponse> getCycleReportData(
            @Parameter(description = "循环ID", required = true, example = "1") @PathVariable Long cycleId) {
        log.info("获取循环报表数据，循环ID: {}", cycleId);
        CycleReportDataResponse response = cycleService.getCycleReportData(cycleId);
        return Result.success(response);
    }
    
    @Operation(summary = "更新循环信息", description = "修改循环的控制时长、开始结束时间、状态、进尺、围岩等级等信息。如果更新为进行中状态，会检查该工点是否已有其他进行中的循环。", tags = {"管理员管理-循环管理"})
    @PutMapping("/cycles/{cycleId}")
    public Result<CycleResponse> updateCycle(
            @Parameter(description = "循环ID", required = true, example = "1") @PathVariable Long cycleId,
            @Valid @RequestBody UpdateCycleRequest request) {
        CycleResponse response = cycleService.updateCycle(cycleId, request);
        return Result.success(response);
    }

    @Operation(summary = "删除循环", description = "删除指定循环，并同时删除该循环下的所有工序（逻辑删除）。仅系统管理员可以使用此接口。", tags = {"管理员管理-循环管理"})
    @DeleteMapping("/cycles/{cycleId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<Void> deleteCycle(
            @Parameter(description = "循环ID", required = true, example = "1") @PathVariable Long cycleId) {
        log.info("删除循环及其工序，循环ID: {}", cycleId);
        cycleService.deleteCycle(cycleId);
        return Result.success();
    }
    
    @Operation(summary = "获取当前循环进行中工序顺序", description = "根据循环ID获取该循环下进行中工序的start_order（应该只有一个进行中的工序）。", tags = {"管理员管理-循环管理"})
    @GetMapping("/cycles/{cycleId}/in-progress-process-orders")
    public Result<InProgressProcessOrderResponse> getInProgressProcessOrder(
            @Parameter(description = "循环ID", required = true, example = "1") @PathVariable Long cycleId) {
        log.info("获取当前循环进行中工序顺序，循环ID: {}", cycleId);
        InProgressProcessOrderResponse response = cycleService.getInProgressProcessOrder(cycleId);
        return Result.success(response);
    }
    
    // 授权接口已废弃，补填循环可以直接调用，无需授权
    // @Operation(summary = "授权循环时间补填", 
    //          description = "系统管理员授权指定循环的时间补填。补填循环前必须先授权。", 
    //          tags = {"管理员管理-循环管理"})
    // @PostMapping("/cycles/{cycleId}/authorize-time-fill")
    // @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    // public Result<Void> authorizeCycleTimeFill(
    //         @Parameter(description = "循环ID", required = true, example = "1") 
    //         @PathVariable Long cycleId) {
    //     log.info("系统管理员授权循环时间补填，循环ID: {}", cycleId);
    //     cycleService.authorizeCycleTimeFill(cycleId);
    //     return Result.success("授权成功");
    // }
}

