package com.zzw.zzwgx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzw.zzwgx.common.enums.ProcessStatus;
import com.zzw.zzwgx.common.enums.ResultCode;
import com.zzw.zzwgx.common.enums.TaskStatus;
import com.zzw.zzwgx.common.exception.BusinessException;
import com.zzw.zzwgx.dto.request.CompleteTaskRequest;
import com.zzw.zzwgx.dto.response.TaskDetailResponse;
import com.zzw.zzwgx.dto.response.TaskListResponse;
import com.zzw.zzwgx.entity.Cycle;
import com.zzw.zzwgx.entity.Process;
import com.zzw.zzwgx.entity.Project;
import com.zzw.zzwgx.entity.Task;
import com.zzw.zzwgx.mapper.ProcessMapper;
import com.zzw.zzwgx.mapper.TaskMapper;
import com.zzw.zzwgx.service.CycleService;
import com.zzw.zzwgx.service.ProjectService;
import com.zzw.zzwgx.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {
    
    private final ProcessMapper processMapper;
    private final CycleService cycleService;
    private final ProjectService projectService;
    
    @Override
    public Page<Task> getTaskPageByWorkerId(Long workerId, Integer pageNum, Integer pageSize, String projectName) {
        log.debug("分页查询施工人员任务，施工人员ID: {}, 页码: {}, 每页大小: {}, 项目名称: {}", workerId, pageNum, pageSize, projectName);
        Page<Task> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getWorkerId, workerId);
        // 这里可以根据项目名称关联查询，简化处理
        wrapper.orderByDesc(Task::getCreateTime);
        Page<Task> result = page(page, wrapper);
        log.debug("分页查询施工人员任务完成，施工人员ID: {}, 共查询到 {} 条记录", workerId, result.getTotal());
        return result;
    }
    
    @Override
    public Page<TaskListResponse> getTaskList(Long workerId, Integer pageNum, Integer pageSize, String projectName) {
        log.info("查询任务列表，施工人员ID: {}, 页码: {}, 每页大小: {}, 项目名称: {}", workerId, pageNum, pageSize, projectName);
        Page<Task> page = getTaskPageByWorkerId(workerId, pageNum, pageSize, projectName);
        Page<TaskListResponse> responsePage = new Page<>(pageNum, pageSize, page.getTotal());
        
        List<TaskListResponse> list = page.getRecords().stream().map(task -> {
            TaskListResponse response = new TaskListResponse();
            response.setId(task.getId());
            response.setStatus(task.getStatus());
            TaskStatus status = TaskStatus.fromCode(task.getStatus());
            response.setStatusDesc(status != null ? status.getDesc() : "");
            
            // 直接使用Mapper避免循环依赖
            Process process = processMapper.selectById(task.getProcessId());
            if (process != null) {
                response.setTaskName(process.getName());
                response.setTaskTime(process.getControlTime());
                
                Cycle cycle = cycleService.getById(process.getCycleId());
                if (cycle != null) {
                    response.setCurrentCycle(cycle.getCycleNumber());
                    
                    Project project = projectService.getById(cycle.getProjectId());
                    if (project != null) {
                        response.setProjectName(project.getName());
                    }
                }
            }
            
            return response;
        }).collect(Collectors.toList());
        
        responsePage.setRecords(list);
        log.info("查询任务列表成功，施工人员ID: {}, 共查询到 {} 条记录", workerId, list.size());
        return responsePage;
    }
    
    @Override
    public TaskDetailResponse getTaskDetail(Long taskId, Long workerId) {
        log.info("查询任务详情，任务ID: {}, 施工人员ID: {}", taskId, workerId);
        Task task = getById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }
        
        if (!task.getWorkerId().equals(workerId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        
        TaskDetailResponse response = new TaskDetailResponse();
        response.setId(task.getId());
        response.setStatus(task.getStatus());
        TaskStatus status = TaskStatus.fromCode(task.getStatus());
        response.setStatusDesc(status != null ? status.getDesc() : "");
        response.setEstimatedStartTime(task.getEstimatedStartTime());
        response.setActualStartTime(task.getActualStartTime());
        response.setActualEndTime(task.getActualEndTime());
        
        // 直接使用Mapper避免循环依赖
        Process process = processMapper.selectById(task.getProcessId());
        if (process != null) {
            response.setTaskName(process.getName());
            response.setTaskTime(process.getControlTime());
            
            Cycle cycle = cycleService.getById(process.getCycleId());
            if (cycle != null) {
                response.setCurrentCycle(cycle.getCycleNumber());
                
                // 获取上一工序 - 直接使用Mapper查询
                if (process.getStartOrder() > 1) {
                    LambdaQueryWrapper<Process> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(Process::getCycleId, process.getCycleId())
                            .lt(Process::getStartOrder, process.getStartOrder())
                            .orderByDesc(Process::getStartOrder)
                            .last("LIMIT 1");
                    Process previousProcess = processMapper.selectOne(wrapper);
                    if (previousProcess != null) {
                        response.setPreviousProcess(previousProcess.getName());
                        response.setPreviousProcessStatus(previousProcess.getStatus());
                    }
                }
            }
            
            // 计算已进行时间
            if (task.getActualStartTime() != null && TaskStatus.IN_PROGRESS.getCode().equals(task.getStatus())) {
                long minutes = Duration.between(task.getActualStartTime(), java.time.LocalDateTime.now()).toMinutes();
                response.setElapsedTime((int) minutes);
            }
        }
        
        log.info("查询任务详情成功，任务ID: {}, 任务名称: {}", taskId, response.getTaskName());
        return response;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startTask(Long taskId, Long workerId) {
        log.info("开始任务，任务ID: {}, 施工人员ID: {}", taskId, workerId);
        Task task = getById(taskId);
        if (task == null) {
            log.error("开始任务失败，任务不存在，任务ID: {}", taskId);
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }
        
        if (!task.getWorkerId().equals(workerId)) {
            log.warn("开始任务失败，权限不足，任务ID: {}, 任务所属施工人员ID: {}, 当前施工人员ID: {}", taskId, task.getWorkerId(), workerId);
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        
        if (TaskStatus.IN_PROGRESS.getCode().equals(task.getStatus())) {
            log.warn("开始任务失败，任务已开始，任务ID: {}", taskId);
            throw new BusinessException(ResultCode.TASK_ALREADY_STARTED);
        }
        
        if (TaskStatus.COMPLETED.getCode().equals(task.getStatus())) {
            log.warn("开始任务失败，任务已完成，任务ID: {}", taskId);
            throw new BusinessException(ResultCode.TASK_ALREADY_COMPLETED);
        }
        
        // 检查上一工序是否完成
        Process process = processMapper.selectById(task.getProcessId());
        if (process != null && process.getStartOrder() > 1) {
            // 查询上一工序
            LambdaQueryWrapper<Process> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Process::getCycleId, process.getCycleId())
                    .lt(Process::getStartOrder, process.getStartOrder())
                    .orderByDesc(Process::getStartOrder)
                    .last("LIMIT 1");
            Process previousProcess = processMapper.selectOne(wrapper);
            if (previousProcess != null && !ProcessStatus.COMPLETED.getCode().equals(previousProcess.getStatus())) {
                log.warn("开始任务失败，上一工序未完成，任务ID: {}, 上一工序ID: {}, 上一工序状态: {}", taskId, previousProcess.getId(), previousProcess.getStatus());
                throw new BusinessException(ResultCode.PREVIOUS_PROCESS_NOT_COMPLETED);
            }
        }
        
        task.setStatus(TaskStatus.IN_PROGRESS.getCode());
        task.setActualStartTime(LocalDateTime.now());
        updateById(task);
        log.info("任务开始成功，任务ID: {}, 开始时间: {}", taskId, task.getActualStartTime());
        
        // 更新工序状态
        process.setStatus(ProcessStatus.IN_PROGRESS.getCode());
        process.setActualStartTime(LocalDateTime.now());
        processMapper.updateById(process);
        log.debug("更新工序状态为进行中，工序ID: {}, 开始时间: {}", process.getId(), process.getActualStartTime());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long taskId, Long workerId, CompleteTaskRequest request) {
        log.info("完成任务，任务ID: {}, 施工人员ID: {}", taskId, workerId);
        Task task = getById(taskId);
        if (task == null) {
            log.error("完成任务失败，任务不存在，任务ID: {}", taskId);
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }
        
        if (!task.getWorkerId().equals(workerId)) {
            log.warn("完成任务失败，权限不足，任务ID: {}, 任务所属施工人员ID: {}, 当前施工人员ID: {}", taskId, task.getWorkerId(), workerId);
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        
        if (!TaskStatus.IN_PROGRESS.getCode().equals(task.getStatus())) {
            log.warn("完成任务失败，任务未开始，任务ID: {}, 任务状态: {}", taskId, task.getStatus());
            throw new BusinessException(ResultCode.TASK_NOT_STARTED);
        }
        
        Process process = processMapper.selectById(task.getProcessId());
        if (process == null) {
            log.error("完成任务失败，工序不存在，任务ID: {}, 工序ID: {}", taskId, task.getProcessId());
            throw new BusinessException(ResultCode.PROCESS_NOT_FOUND);
        }
        
        LocalDateTime endTime = LocalDateTime.now();
        task.setActualEndTime(endTime);
        task.setStatus(TaskStatus.COMPLETED.getCode());
        
        // 计算实际时间（分钟）
        long actualMinutes = 0;
        if (task.getActualStartTime() != null) {
            actualMinutes = Duration.between(task.getActualStartTime(), endTime).toMinutes();
        }
        log.debug("计算任务实际时间，任务ID: {}, 实际时间: {} 分钟，控制时间: {} 分钟", taskId, actualMinutes, process.getControlTime());
        
        // 检查是否超时
        if (process.getControlTime() != null && actualMinutes > process.getControlTime()) {
            log.warn("任务超时，任务ID: {}, 实际时间: {} 分钟，控制时间: {} 分钟，超时: {} 分钟", 
                    taskId, actualMinutes, process.getControlTime(), actualMinutes - process.getControlTime());
            if (!StringUtils.hasText(request.getOvertimeReason())) {
                log.error("完成任务失败，超时但未填写超时原因，任务ID: {}", taskId);
                throw new BusinessException(ResultCode.OVERTIME_REASON_REQUIRED);
            }
            task.setOvertimeReason(request.getOvertimeReason());
        } else if (process.getControlTime() != null && actualMinutes < process.getControlTime()) {
            long savedMinutes = process.getControlTime() - actualMinutes;
            log.info("任务提前完成，任务ID: {}, 节省时间: {} 分钟", taskId, savedMinutes);
        }
        
        updateById(task);
        log.info("任务完成成功，任务ID: {}, 结束时间: {}", taskId, endTime);
        
        // 更新工序状态
        process.setStatus(ProcessStatus.COMPLETED.getCode());
        process.setActualEndTime(endTime);
        processMapper.updateById(process);
        log.debug("更新工序状态为已完成，工序ID: {}, 结束时间: {}", process.getId(), endTime);
        
        // 更新循环进尺
        // 这里需要计算循环的总进尺，简化处理
    }
}

