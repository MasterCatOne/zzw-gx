package com.zzw.zzwgx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzw.zzwgx.common.enums.ProcessStatus;
import com.zzw.zzwgx.dto.response.StatisticsResponse;
import com.zzw.zzwgx.entity.Cycle;
import com.zzw.zzwgx.entity.Process;
import com.zzw.zzwgx.entity.Project;
import com.zzw.zzwgx.entity.Task;
import com.zzw.zzwgx.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * 统计服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    
    private final ProjectService projectService;
    private final CycleService cycleService;
    private final ProcessService processService;
    private final TaskService taskService;
    
    @Override
    public StatisticsResponse.ProcessTimeStat getProcessTimeStatistics(Long projectId, Integer year, Integer month) {
        log.info("计算工点工序总时间统计，项目ID: {}, 年份: {}, 月份: {}", projectId, year, month);
        Project project = projectService.getById(projectId);
        if (project == null) {
            log.warn("计算工点工序总时间统计失败，项目不存在，项目ID: {}", projectId);
            return null;
        }
        
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.with(TemporalAdjusters.lastDayOfMonth());
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        log.debug("统计时间范围，开始时间: {}, 结束时间: {}", startDateTime, endDateTime);
        
        // 获取该时间段内的循环
        List<Cycle> cycles = cycleService.list(new LambdaQueryWrapper<Cycle>()
                .eq(Cycle::getProjectId, projectId)
                .between(Cycle::getStartDate, startDateTime, endDateTime));
        log.debug("查询到循环数量: {}", cycles.size());
        
        double totalControlTime = 0;
        double totalActualTime = 0;
        int count = 0;
        
        for (Cycle cycle : cycles) {
            List<Process> processes = processService.getProcessesByCycleId(cycle.getId());
            for (Process process : processes) {
                if (ProcessStatus.COMPLETED.getCode().equals(process.getStatus())) {
                    totalControlTime += process.getControlTime();
                    if (process.getActualStartTime() != null && process.getActualEndTime() != null) {
                        long minutes = Duration.between(process.getActualStartTime(), process.getActualEndTime()).toMinutes();
                        totalActualTime += minutes;
                    }
                    count++;
                }
            }
        }
        
        StatisticsResponse.ProcessTimeStat stat = new StatisticsResponse.ProcessTimeStat();
        stat.setProjectName(project.getName());
        if (count > 0) {
            stat.setAverageTime(totalActualTime / count / 60.0); // 转换为小时
            stat.setSavedTime(Math.max(0, (totalControlTime - totalActualTime) / 60.0)); // 转换为小时
        } else {
            stat.setAverageTime(0.0);
            stat.setSavedTime(0.0);
        }
        log.info("工点工序总时间统计计算完成，项目ID: {}, 平均时间: {} 小时，节省时间: {} 小时", 
                projectId, stat.getAverageTime(), stat.getSavedTime());
        
        return stat;
    }
    
    @Override
    public StatisticsResponse.AdvanceLengthStat getAdvanceLengthStatistics(Long projectId, Integer year, Integer month) {
        log.info("计算进总尺长度统计，项目ID: {}, 年份: {}, 月份: {}", projectId, year, month);
        Project project = projectService.getById(projectId);
        if (project == null) {
            log.warn("计算进总尺长度统计失败，项目不存在，项目ID: {}", projectId);
            return null;
        }
        
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.with(TemporalAdjusters.lastDayOfMonth());
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        List<Cycle> cycles = cycleService.list(new LambdaQueryWrapper<Cycle>()
                .eq(Cycle::getProjectId, projectId)
                .between(Cycle::getStartDate, startDateTime, endDateTime));
        log.debug("查询到循环数量: {}", cycles.size());
        
        double totalAdvanceLength = 0;
        int cycleCount = cycles.size();
        
        for (Cycle cycle : cycles) {
            if (cycle.getAdvanceLength() != null) {
                totalAdvanceLength += cycle.getAdvanceLength().doubleValue();
            }
        }
        
        StatisticsResponse.AdvanceLengthStat stat = new StatisticsResponse.AdvanceLengthStat();
        stat.setProjectName(project.getName());
        stat.setCycleCount(cycleCount);
        stat.setAdvanceLength(totalAdvanceLength);
        log.info("进总尺长度统计计算完成，项目ID: {}, 循环数量: {}, 总进尺: {} 米", 
                projectId, cycleCount, totalAdvanceLength);
        
        return stat;
    }
    
    @Override
    public StatisticsResponse.OvertimeStat getOvertimeStatistics(Long projectId) {
        log.info("计算本周超耗总时间统计，项目ID: {}", projectId);
        Project project = projectService.getById(projectId);
        if (project == null) {
            log.warn("计算本周超耗总时间统计失败，项目不存在，项目ID: {}", projectId);
            return null;
        }
        
        LocalDate now = LocalDate.now();
        LocalDate weekStart = now.minusDays(now.getDayOfWeek().getValue() - 1);
        LocalDate weekEnd = weekStart.plusDays(6);
        LocalDateTime weekStartDateTime = weekStart.atStartOfDay();
        LocalDateTime weekEndDateTime = weekEnd.atTime(23, 59, 59);
        log.debug("本周时间范围，开始时间: {}, 结束时间: {}", weekStartDateTime, weekEndDateTime);
        
        List<Cycle> cycles = cycleService.list(new LambdaQueryWrapper<Cycle>()
                .eq(Cycle::getProjectId, projectId)
                .between(Cycle::getStartDate, weekStartDateTime, weekEndDateTime));
        log.debug("查询到循环数量: {}", cycles.size());
        
        double totalOvertime = 0;
        double totalSavedTime = 0;
        
        for (Cycle cycle : cycles) {
            List<Process> processes = processService.getProcessesByCycleId(cycle.getId());
            for (Process process : processes) {
                if (ProcessStatus.COMPLETED.getCode().equals(process.getStatus())) {
                    if (process.getActualStartTime() != null && process.getActualEndTime() != null) {
                        long minutes = Duration.between(process.getActualStartTime(), process.getActualEndTime()).toMinutes();
                        if (minutes > process.getControlTime()) {
                            totalOvertime += (minutes - process.getControlTime()) / 60.0;
                        } else {
                            totalSavedTime += (process.getControlTime() - minutes) / 60.0;
                        }
                    }
                }
            }
        }
        
        StatisticsResponse.OvertimeStat stat = new StatisticsResponse.OvertimeStat();
        stat.setProjectName(project.getName());
        stat.setOvertime(totalOvertime);
        stat.setSavedTime(totalSavedTime);
        log.info("本周超耗总时间统计计算完成，项目ID: {}, 超时时间: {} 小时，节省时间: {} 小时", 
                projectId, totalOvertime, totalSavedTime);
        
        return stat;
    }
    
    @Override
    public StatisticsResponse.OvertimeStat getWorkerOvertimeStatistics(Long workerId) {
        log.info("计算施工人员超耗总时间统计，施工人员ID: {}", workerId);
        LocalDate now = LocalDate.now();
        LocalDate weekStart = now.minusDays(now.getDayOfWeek().getValue() - 1);
        LocalDate weekEnd = weekStart.plusDays(6);
        LocalDateTime weekStartDateTime = weekStart.atStartOfDay();
        LocalDateTime weekEndDateTime = weekEnd.atTime(23, 59, 59);
        log.debug("本周时间范围，开始时间: {}, 结束时间: {}", weekStartDateTime, weekEndDateTime);
        
        List<Task> tasks = taskService.list(new LambdaQueryWrapper<Task>()
                .eq(Task::getWorkerId, workerId)
                .eq(Task::getStatus, "COMPLETED")
                .between(Task::getActualEndTime, weekStartDateTime, weekEndDateTime));
        log.debug("查询到任务数量: {}", tasks.size());
        
        double totalOvertime = 0;
        double totalSavedTime = 0;
        
        for (Task task : tasks) {
            Process process = processService.getById(task.getProcessId());
            if (process != null && task.getActualStartTime() != null && task.getActualEndTime() != null) {
                long minutes = Duration.between(task.getActualStartTime(), task.getActualEndTime()).toMinutes();
                if (minutes > process.getControlTime()) {
                    totalOvertime += (minutes - process.getControlTime()) / 60.0;
                } else {
                    totalSavedTime += (process.getControlTime() - minutes) / 60.0;
                }
            }
        }
        
        StatisticsResponse.OvertimeStat stat = new StatisticsResponse.OvertimeStat();
        stat.setProjectName("我的任务");
        stat.setOvertime(totalOvertime);
        stat.setSavedTime(totalSavedTime);
        log.info("施工人员超耗总时间统计计算完成，施工人员ID: {}, 超时时间: {} 小时，节省时间: {} 小时", 
                workerId, totalOvertime, totalSavedTime);
        
        return stat;
    }
}

