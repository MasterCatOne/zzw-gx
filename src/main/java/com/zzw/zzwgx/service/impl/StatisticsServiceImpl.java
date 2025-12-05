package com.zzw.zzwgx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzw.zzwgx.common.enums.ProcessStatus;
import com.zzw.zzwgx.dto.response.MonthlyStatisticsResponse;
import com.zzw.zzwgx.dto.response.StatisticsResponse;
import com.zzw.zzwgx.dto.response.WeeklyOvertimeSummaryResponse;
import com.zzw.zzwgx.entity.Cycle;
import com.zzw.zzwgx.entity.Process;
import com.zzw.zzwgx.entity.Project;
import com.zzw.zzwgx.entity.User;
import com.zzw.zzwgx.security.SecurityUtils;
import com.zzw.zzwgx.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
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
    private final UserService userService;
    private final UserProjectService userProjectService;
    
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
                if (ProcessStatus.COMPLETED.getCode().equals(process.getProcessStatus())) {
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
        stat.setProjectName(project.getProjectName());
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
        stat.setProjectName(project.getProjectName());
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
                if (ProcessStatus.COMPLETED.getCode().equals(process.getProcessStatus())) {
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
        stat.setProjectName(project.getProjectName());
        stat.setOvertime(totalOvertime);
        stat.setSavedTime(totalSavedTime);
        log.info("本周超耗总时间统计计算完成，项目ID: {}, 超时时间: {} 小时，节省时间: {} 小时", 
                projectId, totalOvertime, totalSavedTime);
        
        return stat;
    }
    
    @Override
    public MonthlyStatisticsResponse getMonthlyStatistics(String month) {
        log.info("计算月度统计，月份: {}", month);
        
        // 解析月份字符串（格式：2025-06）
        int year, monthValue;
        try {
            String[] parts = month.split("-");
            if (parts.length != 2) {
                throw new IllegalArgumentException("月份格式错误，应为：2025-06");
            }
            year = Integer.parseInt(parts[0]);
            monthValue = Integer.parseInt(parts[1]);
            if (monthValue < 1 || monthValue > 12) {
                throw new IllegalArgumentException("月份必须在1-12之间");
            }
        } catch (Exception e) {
            log.error("解析月份失败，月份: {}, 错误: {}", month, e.getMessage());
            throw new IllegalArgumentException("月份格式错误，应为：2025-06", e);
        }
        
        // 计算时间范围
        LocalDate startDate = LocalDate.of(year, monthValue, 1);
        LocalDate endDate = startDate.with(TemporalAdjusters.lastDayOfMonth());
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        log.debug("统计时间范围，开始时间: {}, 结束时间: {}", startDateTime, endDateTime);
        
        // 根据用户权限获取工点列表
        List<Project> sites = getAccessibleSites();
        log.debug("查询到工点数量: {}", sites.size());
        
        MonthlyStatisticsResponse response = new MonthlyStatisticsResponse();
        response.setMonth(month);
        List<MonthlyStatisticsResponse.ProjectStatistics> projectStatsList = new ArrayList<>();
        
        // 遍历每个工点进行统计
        for (Project site : sites) {
            MonthlyStatisticsResponse.ProjectStatistics projectStat = new MonthlyStatisticsResponse.ProjectStatistics();
            projectStat.setProjectId(site.getId());
            projectStat.setProjectName(site.getProjectName());
            
            // 获取该时间段内的循环
            List<Cycle> cycles = cycleService.list(new LambdaQueryWrapper<Cycle>()
                    .eq(Cycle::getProjectId, site.getId())
                    .between(Cycle::getStartDate, startDateTime, endDateTime));
            
            // 统计进尺长度
            BigDecimal totalAdvanceLength = BigDecimal.ZERO;
            for (Cycle cycle : cycles) {
                if (cycle.getAdvanceLength() != null) {
                    totalAdvanceLength = totalAdvanceLength.add(cycle.getAdvanceLength());
                }
            }
            projectStat.setAdvanceLength(totalAdvanceLength);
            
            // 统计工序时间和超耗信息
            double totalProcessTime = 0.0; // 总时长（小时）
            double totalOvertime = 0.0; // 超耗时间（小时）
            List<MonthlyStatisticsResponse.OvertimeDetail> overtimeDetails = new ArrayList<>();
            
            for (Cycle cycle : cycles) {
                List<Process> processes = processService.getProcessesByCycleId(cycle.getId());
                for (Process process : processes) {
                    if (ProcessStatus.COMPLETED.getCode().equals(process.getProcessStatus()) 
                            && process.getActualStartTime() != null 
                            && process.getActualEndTime() != null) {
                        long actualMinutes = Duration.between(
                                process.getActualStartTime(), 
                                process.getActualEndTime()).toMinutes();
                        double actualHours = actualMinutes / 60.0;
                        totalProcessTime += actualHours;
                        
                        // 检查是否超时
                        if (process.getControlTime() != null && actualMinutes > process.getControlTime()) {
                            long overtimeMinutes = actualMinutes - process.getControlTime();
                            double overtimeHours = overtimeMinutes / 60.0;
                            totalOvertime += overtimeHours;
                            
                            // 添加到超耗详情列表
                            MonthlyStatisticsResponse.OvertimeDetail detail = 
                                    new MonthlyStatisticsResponse.OvertimeDetail();
                            detail.setProcessId(process.getId());
                            detail.setProcessName(process.getProcessName());
                            detail.setCycleNumber(cycle.getCycleNumber());
                            detail.setControlTime(process.getControlTime());
                            detail.setActualTime((int) actualMinutes);
                            detail.setOvertimeMinutes((int) overtimeMinutes);
                            detail.setOvertimeReason(process.getOvertimeReason());
                            detail.setActualStartTime(process.getActualStartTime());
                            detail.setActualEndTime(process.getActualEndTime());
                            
                            // 获取操作员姓名
                            if (process.getOperatorId() != null) {
                                User operator = userService.getById(process.getOperatorId());
                                if (operator != null) {
                                    detail.setOperatorName(operator.getRealName());
                                }
                            }
                            
                            overtimeDetails.add(detail);
                        }
                    }
                }
            }
            
            projectStat.setTotalProcessTime(totalProcessTime);
            projectStat.setOvertime(totalOvertime);
            projectStat.setOvertimeDetails(overtimeDetails);
            
            projectStatsList.add(projectStat);
        }
        
        response.setProjectStatistics(projectStatsList);
        
        log.info("月度统计计算完成，月份: {}, 工点数量: {}", month, projectStatsList.size());
        return response;
    }
    
    /**
     * 根据用户权限获取可访问的工点列表
     * 系统管理员可以查看所有工点，普通管理员只能查看自己管理的工点
     */
    private List<Project> getAccessibleSites() {
        var currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            log.warn("未获取到当前登录用户，返回空列表");
            return new ArrayList<>();
        }
        
        var roles = currentUser.getRoleCodes();
        boolean isSystemAdmin = roles.stream().anyMatch(r -> "SYSTEM_ADMIN".equals(r));
        
        if (isSystemAdmin) {
            // 系统管理员查看所有工点
            log.debug("系统管理员，查询所有工点");
            return projectService.list(new LambdaQueryWrapper<Project>()
                    .eq(Project::getNodeType, "SITE"));
        }
        
        // 普通管理员：只查看自己管理的工点
        Long userId = currentUser.getUserId();
        List<Long> assignedProjectIds = userProjectService.getProjectIdsByUser(userId);
        if (CollectionUtils.isEmpty(assignedProjectIds)) {
            log.debug("普通管理员未分配任何工点，用户ID: {}", userId);
            return new ArrayList<>();
        }
        
        // 将分配的节点ID展开为所有子层级的工点ID
        List<Long> siteProjectIds = expandToSiteProjectIds(assignedProjectIds);
        if (CollectionUtils.isEmpty(siteProjectIds)) {
            log.debug("普通管理员未分配到任何工点，用户ID: {}", userId);
            return new ArrayList<>();
        }
        
        log.debug("普通管理员查询分配的工点，用户ID: {}, 工点数量: {}", userId, siteProjectIds.size());
        return projectService.list(new LambdaQueryWrapper<Project>()
                .eq(Project::getNodeType, "SITE")
                .in(Project::getId, siteProjectIds));
    }
    
    /**
     * 将任意层级的节点展开为所有子层级的工点（node_type=SITE）ID
     */
    private List<Long> expandToSiteProjectIds(List<Long> rootIds) {
        List<Long> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(rootIds)) {
            return result;
        }
        java.util.Queue<Long> queue = new java.util.ArrayDeque<>(rootIds);
        while (!queue.isEmpty()) {
            Long currentId = queue.poll();
            Project project = projectService.getById(currentId);
            if (project == null) {
                continue;
            }
            if ("SITE".equalsIgnoreCase(project.getNodeType())) {
                result.add(project.getId());
            } else {
                List<Project> children = projectService.list(new LambdaQueryWrapper<Project>()
                        .eq(Project::getParentId, currentId));
                if (!children.isEmpty()) {
                    children.forEach(child -> queue.offer(child.getId()));
                }
            }
        }
        return result;
    }
    
    @Override
    public WeeklyOvertimeSummaryResponse getWeeklyOvertimeSummary() {
        log.info("计算每周超耗时间汇总和排名");
        
        // 计算本周时间范围
        LocalDate now = LocalDate.now();
        LocalDate weekStart = now.minusDays(now.getDayOfWeek().getValue() - 1);
        LocalDate weekEnd = weekStart.plusDays(6);
        LocalDateTime weekStartDateTime = weekStart.atStartOfDay();
        LocalDateTime weekEndDateTime = weekEnd.atTime(23, 59, 59);
        log.debug("本周时间范围，开始时间: {}, 结束时间: {}", weekStartDateTime, weekEndDateTime);
        
        // 根据用户权限获取可访问的工点列表
        List<Project> sites = getAccessibleSites();
        log.debug("查询到工点数量: {}", sites.size());
        
        WeeklyOvertimeSummaryResponse response = new WeeklyOvertimeSummaryResponse();
        response.setWeekStartDate(weekStart);
        response.setWeekEndDate(weekEnd);
        
        List<WeeklyOvertimeSummaryResponse.ProjectOvertimeStat> overtimeStats = new ArrayList<>();
        List<WeeklyOvertimeSummaryResponse.ProjectSavedTimeRank> savedTimeRanks = new ArrayList<>();
        
        // 遍历每个工点进行统计
        for (Project site : sites) {
            // 获取该时间段内的循环
            List<Cycle> cycles = cycleService.list(new LambdaQueryWrapper<Cycle>()
                    .eq(Cycle::getProjectId, site.getId())
                    .between(Cycle::getStartDate, weekStartDateTime, weekEndDateTime));
            
            double totalOvertime = 0.0; // 超耗时间（小时）
            double totalSavedTime = 0.0; // 节约时间（小时）
            int completedProcessCount = 0;
            
            for (Cycle cycle : cycles) {
                List<Process> processes = processService.getProcessesByCycleId(cycle.getId());
                for (Process process : processes) {
                    if (ProcessStatus.COMPLETED.getCode().equals(process.getProcessStatus()) 
                            && process.getActualStartTime() != null 
                            && process.getActualEndTime() != null
                            && process.getControlTime() != null) {
                        long actualMinutes = Duration.between(
                                process.getActualStartTime(), 
                                process.getActualEndTime()).toMinutes();
                        
                        completedProcessCount++;
                        
                        if (actualMinutes > process.getControlTime()) {
                            // 超时
                            long overtimeMinutes = actualMinutes - process.getControlTime();
                            totalOvertime += overtimeMinutes / 60.0;
                        } else {
                            // 节约时间
                            long savedMinutes = process.getControlTime() - actualMinutes;
                            totalSavedTime += savedMinutes / 60.0;
                        }
                    }
                }
            }
            
            // 创建超耗统计对象
            WeeklyOvertimeSummaryResponse.ProjectOvertimeStat overtimeStat = 
                    new WeeklyOvertimeSummaryResponse.ProjectOvertimeStat();
            overtimeStat.setProjectId(site.getId());
            overtimeStat.setProjectName(site.getProjectName());
            overtimeStat.setOvertime(totalOvertime);
            overtimeStat.setSavedTime(totalSavedTime);
            overtimeStat.setCompletedProcessCount(completedProcessCount);
            overtimeStats.add(overtimeStat);
            
            // 创建节约时间排名对象
            WeeklyOvertimeSummaryResponse.ProjectSavedTimeRank savedTimeRank = 
                    new WeeklyOvertimeSummaryResponse.ProjectSavedTimeRank();
            savedTimeRank.setProjectId(site.getId());
            savedTimeRank.setProjectName(site.getProjectName());
            savedTimeRank.setSavedTime(totalSavedTime);
            savedTimeRank.setOvertime(totalOvertime);
            savedTimeRank.setCompletedProcessCount(completedProcessCount);
            savedTimeRanks.add(savedTimeRank);
        }
        
        // 按超耗时间降序排列并添加排名
        overtimeStats.sort((a, b) -> Double.compare(b.getOvertime(), a.getOvertime()));
        for (int i = 0; i < overtimeStats.size(); i++) {
            overtimeStats.get(i).setRank(i + 1);
        }
        
        // 按节约时间降序排列并添加排名
        savedTimeRanks.sort((a, b) -> Double.compare(b.getSavedTime(), a.getSavedTime()));
        for (int i = 0; i < savedTimeRanks.size(); i++) {
            savedTimeRanks.get(i).setRank(i + 1);
        }
        
        response.setProjectOvertimeStats(overtimeStats);
        response.setSavedTimeRanks(savedTimeRanks);
        
        log.info("每周超耗时间汇总计算完成，工点数量: {}, 超耗排名数量: {}, 节约排名数量: {}", 
                sites.size(), overtimeStats.size(), savedTimeRanks.size());
        return response;
    }

}

