package com.zzw.zzwgx.controller;

import com.zzw.zzwgx.common.Result;
import com.zzw.zzwgx.dto.response.StatisticsResponse;
import com.zzw.zzwgx.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 统计控制器
 */
@Slf4j
@Tag(name = "统计管理", description = "统计数据相关接口")
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;
    
    @Operation(summary = "获取工点工序总时间统计", description = "根据年月查询指定工点的工序总时间统计。返回该工点在指定月份的平均工序时间和节省的总时间（单位：小时）。")
    @GetMapping("/process-time")
    public Result<StatisticsResponse.ProcessTimeStat> getProcessTimeStatistics(
            @Parameter(description = "工点项目ID", required = true, example = "1") @RequestParam Long projectId,
            @Parameter(description = "年份，4位数字", required = true, example = "2024") @RequestParam Integer year,
            @Parameter(description = "月份，1-12", required = true, example = "1") @RequestParam Integer month) {
        log.info("查询工点工序总时间统计，项目ID: {}, 年份: {}, 月份: {}", projectId, year, month);
        StatisticsResponse.ProcessTimeStat stat = statisticsService.getProcessTimeStatistics(projectId, year, month);
        return Result.success(stat);
    }
    
    @Operation(summary = "获取进尺长度统计", description = "根据年月查询指定工点的进尺长度统计。返回该工点在指定月份的循环次数和总进尺长度（单位：米）。")
    @GetMapping("/advance-length")
    public Result<StatisticsResponse.AdvanceLengthStat> getAdvanceLengthStatistics(
            @Parameter(description = "工点项目ID", required = true, example = "1") @RequestParam Long projectId,
            @Parameter(description = "年份，4位数字", required = true, example = "2024") @RequestParam Integer year,
            @Parameter(description = "月份，1-12", required = true, example = "1") @RequestParam Integer month) {
        log.info("查询进总尺长度统计，项目ID: {}, 年份: {}, 月份: {}", projectId, year, month);
        StatisticsResponse.AdvanceLengthStat stat = statisticsService.getAdvanceLengthStatistics(projectId, year, month);
        return Result.success(stat);
    }
    
    @Operation(summary = "获取本周超耗总时间统计", description = "查询指定工点本周的超时和节省时间统计（管理员）。返回本周所有已完成工序的超时总时间和节省总时间（单位：小时）。")
    @GetMapping("/overtime-week")
    public Result<StatisticsResponse.OvertimeStat> getOvertimeStatistics(
            @Parameter(description = "工点项目ID", required = true, example = "1") @RequestParam Long projectId) {
        log.info("查询本周超耗总时间统计，项目ID: {}", projectId);
        StatisticsResponse.OvertimeStat stat = statisticsService.getOvertimeStatistics(projectId);
        return Result.success(stat);
    }

}

