package com.zzw.zzwgx.controller;

import com.zzw.zzwgx.common.Result;
import com.zzw.zzwgx.dto.response.MonthlyStatisticsResponse;
import com.zzw.zzwgx.dto.response.StatisticsResponse;
import com.zzw.zzwgx.dto.response.WeeklyOvertimeSummaryResponse;
import com.zzw.zzwgx.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    
    @Operation(summary = "获取工点工序总时间统计", description = "根据年月查询当前管理员可管理工点的工序总时间统计列表，返回各工点在指定月份的平均工序时间和节省的总时间（单位：小时）。")
    @GetMapping("/process-time")
    public Result<List<StatisticsResponse.ProcessTimeStat>> getProcessTimeStatistics(
            @Parameter(description = "年份，4位数字", required = true, example = "2024") @RequestParam Integer year,
            @Parameter(description = "月份，1-12", required = true, example = "1") @RequestParam Integer month) {
        log.info("查询当前管理员可管理工点的工序总时间统计列表，年份: {}, 月份: {}", year, month);
        List<StatisticsResponse.ProcessTimeStat> stats = statisticsService.getProcessTimeStatisticsForCurrentUser(year, month);
        return Result.success(stats);
    }
    
    @Operation(summary = "获取进尺长度统计", description = "根据年月查询指定工点的进尺长度统计。返回该工点在指定月份的循环次数和总进尺长度（单位：米）。")
    @GetMapping("/advance-length")
    public Result<List<StatisticsResponse.AdvanceLengthStat>> getAdvanceLengthStatistics(
            @Parameter(description = "年份，4位数字", required = true, example = "2024") @RequestParam Integer year,
            @Parameter(description = "月份，1-12", required = true, example = "1") @RequestParam Integer month) {
        log.info("查询当前管理员可管理工点的进尺长度统计列表，年份: {}, 月份: {}", year, month);
        List<StatisticsResponse.AdvanceLengthStat> stats = statisticsService.getAdvanceLengthStatisticsForCurrentUser(year, month);
        return Result.success(stats);
    }
    
    @Operation(summary = "获取本周超耗总时间统计", description = "查询指定工点本周的超时和节省时间统计（管理员）。返回本周所有已完成工序的超时总时间和节省总时间（单位：小时）。")
    @GetMapping("/overtime-week")
    public Result<List<StatisticsResponse.OvertimeStat>> getOvertimeStatistics() {
        log.info("查询当前管理员可管理工点的本周超耗统计列表");
        List<StatisticsResponse.OvertimeStat> stats = statisticsService.getOvertimeStatisticsForCurrentUser();
        return Result.success(stats);
    }
    
//    @Operation(summary = "获取月度统计", description = "按月统计各工点的工序时间总和和进尺长度。返回该月份所有工点的统计数据，包括工序总时长、超耗时间、进尺长度和超耗详情。前端使用2025-06格式选择日期。")
//    @GetMapping("/monthly")
//    public Result<MonthlyStatisticsResponse> getMonthlyStatistics(
//            @Parameter(description = "月份，格式：2025-06", required = true, example = "2025-06") @RequestParam String month) {
//        log.info("查询月度统计，月份: {}", month);
//        MonthlyStatisticsResponse response = statisticsService.getMonthlyStatistics(month);
//        return Result.success(response);
//    }
    
//    @Operation(summary = "获取每周超耗时间汇总和排名", description = "自动汇总各工点本周的超耗时间总和和节约时间，并根据超耗时间和节约时间进行排名。系统管理员查看全部工点，普通管理员只能查看自己管理的工点。")
//    @GetMapping("/weekly-overtime-summary")
//    public Result<WeeklyOvertimeSummaryResponse> getWeeklyOvertimeSummary() {
//        log.info("查询每周超耗时间汇总和排名");
//        WeeklyOvertimeSummaryResponse response = statisticsService.getWeeklyOvertimeSummary();
//        return Result.success(response);
//    }

}

