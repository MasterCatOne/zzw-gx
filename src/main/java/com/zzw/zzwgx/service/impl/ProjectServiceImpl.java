package com.zzw.zzwgx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzw.zzwgx.common.enums.ProcessStatus;
import com.zzw.zzwgx.common.enums.ProjectStatus;
import com.zzw.zzwgx.common.enums.ResultCode;
import com.zzw.zzwgx.common.enums.RockLevel;
import com.zzw.zzwgx.common.exception.BusinessException;
import com.zzw.zzwgx.dto.response.ProjectListResponse;
import com.zzw.zzwgx.dto.response.ProgressDetailResponse;
import com.zzw.zzwgx.entity.Cycle;
import com.zzw.zzwgx.entity.Process;
import com.zzw.zzwgx.entity.Project;
import com.zzw.zzwgx.mapper.ProjectMapper;
import com.zzw.zzwgx.service.CycleService;
import com.zzw.zzwgx.service.ProcessService;
import com.zzw.zzwgx.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {
    
    private final CycleService cycleService;
    private final ProcessService processService;
    
    @Override
    public Page<Project> getProjectPage(Integer pageNum, Integer pageSize, String name) {
        log.debug("分页查询项目，页码: {}, 每页大小: {}, 搜索关键词: {}", pageNum, pageSize, name);
        Page<Project> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(Project::getName, name);
        }
        wrapper.orderByDesc(Project::getCreateTime);
        Page<Project> result = page(page, wrapper);
        log.debug("分页查询项目完成，共查询到 {} 条记录", result.getTotal());
        return result;
    }
    
    @Override
    public Page<ProjectListResponse> getProjectList(Integer pageNum, Integer pageSize, String name) {
        log.info("查询项目列表，页码: {}, 每页大小: {}, 搜索关键词: {}", pageNum, pageSize, name);
        Page<Project> page = getProjectPage(pageNum, pageSize, name);
        Page<ProjectListResponse> responsePage = new Page<>(pageNum, pageSize, page.getTotal());
        
        List<ProjectListResponse> list = page.getRecords().stream().map(project -> {
            ProjectListResponse response = new ProjectListResponse();
            response.setId(project.getId());
            response.setName(project.getName());
            response.setStatus(project.getStatus());
            ProjectStatus status = ProjectStatus.fromCode(project.getStatus());
            response.setStatusDesc(status != null ? status.getDesc() : "");
            response.setCurrentCycle(project.getCurrentCycle());
            response.setRockLevel(project.getRockLevel());
            RockLevel level = RockLevel.fromCode(project.getRockLevel());
            response.setRockLevelDesc(level != null ? level.getDesc() : "");
            return response;
        }).collect(Collectors.toList());
        
        responsePage.setRecords(list);
        log.info("查询项目列表成功，共查询到 {} 条记录", list.size());
        return responsePage;
    }
    
    @Override
    public ProgressDetailResponse getProgressDetail(Long projectId) {
        log.info("查询项目进度详情，项目ID: {}", projectId);
        Project project = getById(projectId);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        
        Cycle currentCycle = cycleService.getCurrentCycleByProjectId(projectId);
        if (currentCycle == null) {
            throw new BusinessException(ResultCode.CYCLE_NOT_FOUND);
        }
        
        Cycle latestCycle = cycleService.getLatestCycleByProjectId(projectId);
        
        ProgressDetailResponse response = new ProgressDetailResponse();
        response.setCycleId(currentCycle.getId());
        response.setCycleNumber(currentCycle.getCycleNumber());
        response.setCycleStatus(currentCycle.getStatus());
        response.setControlDuration(currentCycle.getControlDuration());
        response.setAdvanceLength(currentCycle.getAdvanceLength());
        
        // 获取当前工序
        List<Process> processes = processService.getProcessesByCycleId(currentCycle.getId());
        Process currentProcess = processes.stream()
                .filter(p -> ProcessStatus.IN_PROGRESS.getCode().equals(p.getStatus()))
                .findFirst()
                .orElse(null);
        if (currentProcess != null) {
            response.setCurrentProcess(currentProcess.getName());
        }
        
        // 上循环结束时间
        if (latestCycle != null && latestCycle.getCycleNumber() < currentCycle.getCycleNumber()) {
            List<Process> lastCycleProcesses = processService.getProcessesByCycleId(latestCycle.getId());
            Process lastProcess = lastCycleProcesses.stream()
                    .filter(p -> ProcessStatus.COMPLETED.getCode().equals(p.getStatus()))
                    .reduce((first, second) -> second)
                    .orElse(null);
            if (lastProcess != null && lastProcess.getActualEndTime() != null) {
                response.setLastCycleEndTime(lastProcess.getActualEndTime());
            }
        }
        
        // 本循环开始时间
        response.setCurrentCycleStartTime(currentCycle.getStartDate());
        
        // 工序列表
        List<ProgressDetailResponse.ProcessInfo> processInfos = processes.stream().map(process -> {
            ProgressDetailResponse.ProcessInfo info = new ProgressDetailResponse.ProcessInfo();
            info.setId(process.getId());
            info.setName(process.getName());
            info.setControlTime(process.getControlTime());
            info.setStatus(process.getStatus());
            ProcessStatus status = ProcessStatus.fromCode(process.getStatus());
            info.setStatusDesc(status != null ? status.getDesc() : "");
            info.setActualStartTime(process.getActualStartTime());
            info.setActualEndTime(process.getActualEndTime());
            
            // 计算实际时间
            if (process.getActualStartTime() != null && process.getActualEndTime() != null) {
                long minutes = Duration.between(process.getActualStartTime(), process.getActualEndTime()).toMinutes();
                info.setActualTime((int) minutes);
            }
            
            return info;
        }).collect(Collectors.toList());
        
        response.setProcesses(processInfos);
        log.info("查询项目进度详情成功，项目ID: {}, 循环ID: {}, 工序数量: {}", projectId, currentCycle.getId(), processInfos.size());
        return response;
    }
}

