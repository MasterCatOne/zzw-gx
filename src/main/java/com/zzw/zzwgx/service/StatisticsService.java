package com.zzw.zzwgx.service;

import com.zzw.zzwgx.dto.response.StatisticsResponse;

/**
 * 统计服务接口
 */
public interface StatisticsService {
    
    /**
     * 获取工点工序总时间统计
     */
    StatisticsResponse.ProcessTimeStat getProcessTimeStatistics(Long projectId, Integer year, Integer month);
    
    /**
     * 获取进总尺长度统计
     */
    StatisticsResponse.AdvanceLengthStat getAdvanceLengthStatistics(Long projectId, Integer year, Integer month);
    
    /**
     * 获取本周超耗总时间统计
     */
    StatisticsResponse.OvertimeStat getOvertimeStatistics(Long projectId);
    
    /**
     * 获取施工人员的超耗总时间统计
     */
    StatisticsResponse.OvertimeStat getWorkerOvertimeStatistics(Long workerId);
}

