package com.zzw.zzwgx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzw.zzwgx.common.enums.ProcessStatus;
import com.zzw.zzwgx.common.enums.ResultCode;
import com.zzw.zzwgx.common.exception.BusinessException;
import com.zzw.zzwgx.dto.request.CreateProcessRequest;
import com.zzw.zzwgx.dto.request.UpdateProcessOrderRequest;
import com.zzw.zzwgx.dto.request.UpdateProcessRequest;
import com.zzw.zzwgx.dto.response.ProcessDetailResponse;
import com.zzw.zzwgx.dto.response.ProcessResponse;
import com.zzw.zzwgx.dto.response.WorkerProcessListResponse;
import com.zzw.zzwgx.entity.Cycle;
import com.zzw.zzwgx.entity.Process;
import com.zzw.zzwgx.entity.ProcessTemplate;
import com.zzw.zzwgx.entity.Project;
import com.zzw.zzwgx.entity.User;
import com.zzw.zzwgx.mapper.CycleMapper;
import com.zzw.zzwgx.mapper.ProcessMapper;
import com.zzw.zzwgx.mapper.ProjectMapper;
import com.zzw.zzwgx.service.ProcessService;
import com.zzw.zzwgx.service.ProcessTemplateService;
import com.zzw.zzwgx.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 工序服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessServiceImpl extends ServiceImpl<ProcessMapper, Process> implements ProcessService {
    
    private final CycleMapper cycleMapper;
    private final ProjectMapper projectMapper;
    private final UserService userService;
    private final ProcessTemplateService processTemplateService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessResponse createProcess(CreateProcessRequest request) {
        log.info("开始创建工序，循环ID: {}, 工序名称: {}, 施工人员ID: {}", request.getCycleId(), request.getWorkerId());
        
        // 验证循环是否存在 - 直接使用Mapper避免循环依赖
        Cycle cycle = cycleMapper.selectById(request.getCycleId());
        if (cycle == null) {
            log.error("创建工序失败，循环不存在，循环ID: {}", request.getCycleId());
            throw new BusinessException(ResultCode.CYCLE_NOT_FOUND);
        }
        
        Process process = new Process();
        process.setCycleId(request.getCycleId());
        process.setActualStartTime(request.getActualStartTime());
        process.setProcessStatus(ProcessStatus.NOT_STARTED.getCode());
        process.setOperatorId(request.getWorkerId());
        process.setStartOrder(request.getStartOrder());
        process.setAdvanceLength(java.math.BigDecimal.ZERO);
        
        // 如果指定了模板ID，则从模板中获取工序名称，但控制时长由用户输入
        if (request.getTemplateId() != null) {
            ProcessTemplate template = processTemplateService.getById(request.getTemplateId());
            if (template == null) {
                log.error("创建工序失败，模板不存在，模板ID: {}", request.getTemplateId());
                throw new BusinessException(ResultCode.TEMPLATE_NOT_FOUND);
            }
            // 从模板中获取工序名称，控制时长由用户输入
            process.setProcessName(template.getProcessName());
            if (request.getControlTime() == null) {
                throw new BusinessException("控制时长不能为空");
            }
            process.setControlTime(request.getControlTime());
            process.setTemplateId(request.getTemplateId());
            log.debug("根据模板创建工序，模板ID: {}, 工序名称: {}, 控制时长: {}", 
                    request.getTemplateId(), template.getProcessName(), request.getControlTime());
        } else {
            if (request.getControlTime() == null) {
                throw new BusinessException("控制时长不能为空");
            }
            process.setControlTime(request.getControlTime());
            log.debug("手动创建工序， 控制时长: {}", request.getControlTime());
        }
        
        save(process);
        log.info("工序创建成功，工序ID: {}, 工序名称: {}", process.getId(), process.getProcessName());

        return buildProcessResponse(process);
    }
    
    @Override
    public List<Process> getProcessesByCycleId(Long cycleId) {
        log.debug("查询循环的工序列表，循环ID: {}", cycleId);
        LambdaQueryWrapper<Process> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Process::getCycleId, cycleId)
                .orderByAsc(Process::getStartOrder);
        List<Process> processes = list(wrapper);
        log.debug("查询到工序列表，循环ID: {}, 工序数量: {}", cycleId, processes.size());
        return processes;
    }
    
    @Override
    public Process getPreviousProcess(Long cycleId, Integer startOrder) {
        log.debug("查询上一工序，循环ID: {}, 当前顺序: {}", cycleId, startOrder);
        LambdaQueryWrapper<Process> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Process::getCycleId, cycleId)
                .lt(Process::getStartOrder, startOrder)
                .orderByDesc(Process::getStartOrder)
                .last("LIMIT 1");
        Process process = getOne(wrapper);
        if (process != null) {
            log.debug("查询到上一工序，循环ID: {}, 工序ID: {}, 工序名称: {}", cycleId, process.getId(), process.getProcessName());
        }
        return process;
    }
    
    @Override
    public ProcessDetailResponse getProcessDetail(Long processId) {
        log.info("查询工序详情，工序ID: {}", processId);
        Process process = getById(processId);
        if (process == null) {
            throw new BusinessException(ResultCode.PROCESS_NOT_FOUND);
        }
        ProcessDetailResponse response = buildProcessDetail(process);
        log.info("查询工序详情成功，工序ID: {}, 工序名称: {}", processId, process.getProcessName());
        return response;
    }

    @Override
    public ProcessDetailResponse getWorkerProcessDetail(Long processId, Long workerId) {
        log.info("施工人员查询工序详情，用户ID: {}, 工序ID: {}", workerId, processId);
        Process process = getById(processId);
        if (process == null) {
            throw new BusinessException(ResultCode.PROCESS_NOT_FOUND);
        }
        if (process.getOperatorId() == null || !process.getOperatorId().equals(workerId)) {
            log.warn("没有权限查看该工序详情，工序ID: {}, 用户ID: {}", processId, workerId);
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return buildProcessDetail(process);
    }

    @Override
    public Page<WorkerProcessListResponse> getWorkerProcessList(Long workerId,
                                                                Integer pageNum,
                                                                Integer pageSize,
                                                                String projectName,
                                                                String status) {
        log.info("查询施工人员工序列表，用户ID: {}, 页码: {}, 大小: {}, 工点名称: {}, 状态: {}",
                workerId, pageNum, pageSize, projectName, status);

        // 先按施工人员和状态从工序表中查询
        LambdaQueryWrapper<Process> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Process::getOperatorId, workerId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Process::getProcessStatus, status);
        }
        wrapper.orderByDesc(Process::getCreateTime);

        Page<Process> processPage = page(new Page<>(pageNum, pageSize), wrapper);
        if (processPage.getRecords().isEmpty()) {
            return new Page<>(pageNum, pageSize, 0);
        }

        // 关联查询循环和项目名称
        List<Process> processes = processPage.getRecords();
        List<Long> cycleIds = processes.stream().map(Process::getCycleId).distinct().toList();
        List<Cycle> cycles = cycleIds.isEmpty()
                ? java.util.Collections.emptyList()
                : cycleMapper.selectBatchIds(cycleIds);
        Map<Long, Cycle> cycleMap = cycles.stream().collect(java.util.stream.Collectors.toMap(Cycle::getId, c -> c));

        List<Long> projectIds = cycles.stream().map(Cycle::getProjectId).distinct().toList();
        List<Project> projects = projectIds.isEmpty()
                ? java.util.Collections.emptyList()
                : projectMapper.selectBatchIds(projectIds);
        Map<Long, Project> projectMap = projects.stream().collect(java.util.stream.Collectors.toMap(Project::getId, p -> p));

        // 映射为响应DTO，并在内存中根据项目名称做过滤
        List<WorkerProcessListResponse> all = new java.util.ArrayList<>();
        for (Process p : processes) {
            Cycle cycle = cycleMap.get(p.getCycleId());
            if (cycle == null) {
                continue;
            }
            Project project = projectMap.get(cycle.getProjectId());
            String projName = project != null ? project.getProjectName() : null;

            // 工点名称模糊过滤
            if (projectName != null && !projectName.isEmpty()) {
                if (projName == null || !projName.contains(projectName)) {
                    continue;
                }
            }

            WorkerProcessListResponse resp = new WorkerProcessListResponse();
            resp.setProcessId(p.getId());
            resp.setProjectName(projName);
            resp.setProcessName(p.getProcessName());
            resp.setStatus(p.getProcessStatus());
            ProcessStatus ps = ProcessStatus.fromCode(p.getProcessStatus());
            resp.setStatusDesc(ps != null ? ps.getDesc() : "");
            resp.setCycleNumber(cycle.getCycleNumber());
            resp.setTaskTimeMinutes(p.getControlTime());
            resp.setActualStartTime(p.getActualStartTime());
            resp.setActualEndTime(p.getActualEndTime());

            all.add(resp);
        }

        // 重新根据过滤后的结果做分页
        long total = all.size();
        Page<WorkerProcessListResponse> result = new Page<>(pageNum, pageSize, total);
        int fromIndex = (int) ((pageNum - 1L) * pageSize);
        if (fromIndex >= total) {
            result.setRecords(java.util.Collections.emptyList());
            return result;
        }
        int toIndex = (int) Math.min(fromIndex + pageSize, total);
        result.setRecords(all.subList(fromIndex, toIndex));

        log.info("查询施工人员工序列表完成，用户ID: {}, 共 {} 条，当前页 {} 条", workerId, total, result.getRecords().size());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessResponse updateProcess(Long processId, UpdateProcessRequest request) {
        log.info("更新工序，工序ID: {}", processId);
        Process process = getById(processId);
        if (process == null) {
            throw new BusinessException(ResultCode.PROCESS_NOT_FOUND);
        }

        if (request.getName() != null) {
            process.setProcessName(request.getName());
        }
        if (request.getControlTime() != null) {
            process.setControlTime(request.getControlTime());
        }
        if (request.getEstimatedStartTime() != null) {
            process.setEstimatedStartTime(request.getEstimatedStartTime());
        }
        if (request.getEstimatedEndTime() != null) {
            process.setEstimatedEndTime(request.getEstimatedEndTime());
        }
        if (request.getActualStartTime() != null) {
            process.setActualStartTime(request.getActualStartTime());
        }
        if (request.getActualEndTime() != null) {
            process.setActualEndTime(request.getActualEndTime());
        }
        if (request.getStatus() != null) {
            process.setProcessStatus(request.getStatus());
        }
        if (request.getOperatorId() != null) {
            process.setOperatorId(request.getOperatorId());
        }
        if (request.getStartOrder() != null) {
            process.setStartOrder(request.getStartOrder());
        }
        if (request.getAdvanceLength() != null) {
            process.setAdvanceLength(request.getAdvanceLength());
        }

        updateById(process);

        // 复用创建时的构造逻辑，返回最新数据
        ProcessResponse response = buildProcessResponse(process);

        log.info("更新工序成功，工序ID: {}", processId);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startWorkerProcess(Long processId, Long workerId, LocalDateTime actualStartTime) {
        log.info("施工人员开始工序，用户ID: {}, 工序ID: {}, 实际开始时间: {}", workerId, processId, actualStartTime);
        Process process = getById(processId);
        if (process == null) {
            throw new BusinessException(ResultCode.PROCESS_NOT_FOUND);
        }
        if (process.getOperatorId() == null || !process.getOperatorId().equals(workerId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (!ProcessStatus.NOT_STARTED.getCode().equals(process.getProcessStatus())) {
            throw new BusinessException("当前工序已开始或已完成，无法重复开始");
        }
        if (actualStartTime == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING);
        }

        Process previousProcess = null;
        if (process.getCycleId() != null && process.getStartOrder() != null) {
            previousProcess = getPreviousProcess(process.getCycleId(), process.getStartOrder());
        }
        if (previousProcess != null && !ProcessStatus.COMPLETED.getCode().equals(previousProcess.getProcessStatus())) {
            throw new BusinessException(ResultCode.PREVIOUS_PROCESS_NOT_COMPLETED);
        }

        process.setActualStartTime(actualStartTime);
        process.setProcessStatus(ProcessStatus.IN_PROGRESS.getCode());
        updateById(process);

        log.info("施工人员开始工序成功，工序ID: {}", processId);
    }

    private ProcessDetailResponse buildProcessDetail(Process process) {
        ProcessDetailResponse response = new ProcessDetailResponse();
        response.setId(process.getId());
        response.setName(process.getProcessName());
        response.setStatus(process.getProcessStatus());
        ProcessStatus status = ProcessStatus.fromCode(process.getProcessStatus());
        response.setStatusDesc(status != null ? status.getDesc() : "");
        response.setControlTime(process.getControlTime());
        response.setEstimatedStartTime(process.getEstimatedStartTime());
        response.setActualStartTime(process.getActualStartTime());
        response.setActualEndTime(process.getActualEndTime());

        if (process.getOperatorId() != null) {
            User operator = userService.getById(process.getOperatorId());
            if (operator != null) {
                response.setOperatorName(operator.getRealName());
            }
        }

        Cycle cycle = process.getCycleId() != null ? cycleMapper.selectById(process.getCycleId()) : null;
        if (cycle != null) {
            response.setCycleNumber(cycle.getCycleNumber());
        }

        Process previousProcess = null;
        if (process.getCycleId() != null && process.getStartOrder() != null) {
            previousProcess = getPreviousProcess(process.getCycleId(), process.getStartOrder());
        }
        if (previousProcess != null) {
            response.setPreviousProcessName(previousProcess.getProcessName());
            response.setPreviousProcessStatus(previousProcess.getProcessStatus());
            ProcessStatus prevStatus = ProcessStatus.fromCode(previousProcess.getProcessStatus());
            response.setPreviousProcessStatusDesc(prevStatus != null ? prevStatus.getDesc() : "");
        }

        Integer processTimeMinutes = null;
        if (process.getActualStartTime() != null && process.getActualEndTime() != null) {
            long minutes = Duration.between(process.getActualStartTime(), process.getActualEndTime()).toMinutes();
            processTimeMinutes = (int) minutes;
            response.setFinalTime(processTimeMinutes);

            if (process.getControlTime() != null) {
                if (minutes < process.getControlTime()) {
                    response.setSavedTime(process.getControlTime() - (int) minutes);
                } else if (minutes > process.getControlTime()) {
                    response.setOvertime((int) minutes - process.getControlTime());
                }
            }
        } else if (process.getActualStartTime() != null) {
            long minutes = Duration.between(process.getActualStartTime(), LocalDateTime.now()).toMinutes();
            processTimeMinutes = (int) minutes;
        } else if (process.getControlTime() != null) {
            processTimeMinutes = process.getControlTime();
        }
        response.setProcessTimeMinutes(processTimeMinutes);

        return response;
    }

    private ProcessResponse buildProcessResponse(Process process) {
        ProcessResponse response = new ProcessResponse();
        response.setId(process.getId());
        response.setCycleId(process.getCycleId());
        response.setName(process.getProcessName());
        response.setControlTime(process.getControlTime());
        response.setStatus(process.getProcessStatus());
        ProcessStatus status = ProcessStatus.fromCode(process.getProcessStatus());
        response.setStatusDesc(status != null ? status.getDesc() : "");
        response.setOperatorId(process.getOperatorId());
        response.setStartOrder(process.getStartOrder());
        response.setAdvanceLength(process.getAdvanceLength());
        response.setTemplateId(process.getTemplateId());
        response.setEstimatedStartTime(process.getEstimatedStartTime());
        response.setEstimatedEndTime(process.getEstimatedEndTime());
        response.setActualStartTime(process.getActualStartTime());
        response.setActualEndTime(process.getActualEndTime());
        response.setCreateTime(process.getCreateTime());
        response.setUpdateTime(process.getUpdateTime());

        if (process.getOperatorId() != null) {
            User operator = userService.getById(process.getOperatorId());
            if (operator != null) {
                response.setOperatorName(operator.getRealName());
            }
        }
        return response;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProcessOrders(Long cycleId, UpdateProcessOrderRequest request) {
        log.info("批量更新工序顺序，循环ID: {}", cycleId);
        
        if (request.getProcessOrders() == null || request.getProcessOrders().isEmpty()) {
            throw new BusinessException("工序顺序列表不能为空");
        }
        
        // 验证所有工序都属于该循环
        for (UpdateProcessOrderRequest.ProcessOrderItem item : request.getProcessOrders()) {
            Process process = getById(item.getProcessId());
            if (process == null) {
                throw new BusinessException("工序不存在，工序ID: " + item.getProcessId());
            }
            if (!process.getCycleId().equals(cycleId)) {
                throw new BusinessException("工序不属于该循环，工序ID: " + item.getProcessId());
            }
        }
        
        // 批量更新工序顺序
        for (UpdateProcessOrderRequest.ProcessOrderItem item : request.getProcessOrders()) {
            Process process = getById(item.getProcessId());
            if (process != null) {
                process.setStartOrder(item.getStartOrder());
                updateById(process);
                log.debug("更新工序顺序，工序ID: {}, 新顺序: {}", item.getProcessId(), item.getStartOrder());
            }
        }
        
        log.info("批量更新工序顺序完成，循环ID: {}, 更新数量: {}", cycleId, request.getProcessOrders().size());
    }
}

