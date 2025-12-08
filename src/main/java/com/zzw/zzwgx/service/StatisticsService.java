package com.zzw.zzwgx.service;

import com.zzw.zzwgx.dto.response.MonthlyStatisticsResponse;
import com.zzw.zzwgx.dto.response.StatisticsResponse;
import com.zzw.zzwgx.dto.response.WeeklyOvertimeSummaryResponse;

/**
 * 统计服务接口
 */
public interface StatisticsService {
    
    /**
     * 获取工点工序总时间统计
     */
    StatisticsResponse.ProcessTimeStat getProcessTimeStatistics(Long projectId, Integer year, Integer month);

    /**
     * 获取当前管理员可查看工点的工序总时间统计列表
     */
    java.util.List<StatisticsResponse.ProcessTimeStat> getProcessTimeStatisticsForCurrentUser(Integer year, Integer month);
    
    /**
     * 获取进总尺长度统计
     */
    StatisticsResponse.AdvanceLengthStat getAdvanceLengthStatistics(Long projectId, Integer year, Integer month);

    /**
     * 获取当前管理员可查看工点的进尺长度统计列表
     */
    java.util.List<StatisticsResponse.AdvanceLengthStat> getAdvanceLengthStatisticsForCurrentUser(Integer year, Integer month);
    
    /**
     * 获取本周超耗总时间统计
     */
    StatisticsResponse.OvertimeStat getOvertimeStatistics(Long projectId);

    /**
     * 获取当前管理员可查看工点的本周超耗统计列表
     */
    java.util.List<StatisticsResponse.OvertimeStat> getOvertimeStatisticsForCurrentUser();
    
    /**
     * 获取施工人员本周所在工点的超耗统计列表（按工点汇总）
     */
    java.util.List<StatisticsResponse.OvertimeStat> getWorkerOvertimeStatistics(Long workerId);
    
    /**
     * 获取月度统计（各工点的工序时间总和和进尺长度）
     * @param month 月份，格式：2025-06
     * @return 月度统计数据
     */
    MonthlyStatisticsResponse getMonthlyStatistics(String month);
    
    /**
     * 获取每周超耗时间汇总和排名
     * 自动汇总各工点的超耗时间总和，并根据节约时间和超时时间进行排名
     * @return 每周超耗统计汇总数据
     */
    WeeklyOvertimeSummaryResponse getWeeklyOvertimeSummary();

}

