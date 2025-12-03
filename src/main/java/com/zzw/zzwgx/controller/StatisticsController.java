package com.zzw.zzwgx.controller;

import com.zzw.zzwgx.common.Result;
import com.zzw.zzwgx.dto.response.StatisticsResponse;
import com.zzw.zzwgx.security.SecurityUtils;
import com.zzw.zzwgx.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
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
    
    @Operation(summary = "获取工点工序总时间统计", description = "根据年月查询工点工序总时间统计")
    @GetMapping("/process-time")
    public Result<StatisticsResponse.ProcessTimeStat> getProcessTimeStatistics(
            @RequestParam Long projectId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        log.info("查询工点工序总时间统计，项目ID: {}, 年份: {}, 月份: {}", projectId, year, month);
        StatisticsResponse.ProcessTimeStat stat = statisticsService.getProcessTimeStatistics(projectId, year, month);
        return Result.success(stat);
    }
    
    @Operation(summary = "获取进总尺长度统计", description = "根据年月查询进总尺长度统计")
    @GetMapping("/advance-length")
    public Result<StatisticsResponse.AdvanceLengthStat> getAdvanceLengthStatistics(
            @RequestParam Long projectId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        log.info("查询进总尺长度统计，项目ID: {}, 年份: {}, 月份: {}", projectId, year, month);
        StatisticsResponse.AdvanceLengthStat stat = statisticsService.getAdvanceLengthStatistics(projectId, year, month);
        return Result.success(stat);
    }
    
    @Operation(summary = "获取本周超耗总时间统计", description = "查询本周超耗总时间统计（管理员）")
    @GetMapping("/overtime-week")
    public Result<StatisticsResponse.OvertimeStat> getOvertimeStatistics(@RequestParam Long projectId) {
        log.info("查询本周超耗总时间统计，项目ID: {}", projectId);
        StatisticsResponse.OvertimeStat stat = statisticsService.getOvertimeStatistics(projectId);
        return Result.success(stat);
    }

}

