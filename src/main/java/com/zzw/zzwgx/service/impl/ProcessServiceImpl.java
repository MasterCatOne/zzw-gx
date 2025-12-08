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
import com.zzw.zzwgx.dto.response.StartProcessResponse;
import com.zzw.zzwgx.dto.response.WorkerProcessListResponse;
import com.zzw.zzwgx.entity.Cycle;
import com.zzw.zzwgx.entity.Process;
import com.zzw.zzwgx.entity.ProcessCatalog;
import com.zzw.zzwgx.entity.Project;
import com.zzw.zzwgx.entity.User;
import com.zzw.zzwgx.mapper.CycleMapper;
import com.zzw.zzwgx.mapper.ProcessMapper;
import com.zzw.zzwgx.mapper.ProjectMapper;
import com.zzw.zzwgx.service.CycleService;
import com.zzw.zzwgx.service.ProcessCatalogService;
import com.zzw.zzwgx.service.ProcessService;
import com.zzw.zzwgx.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
    private final ProcessCatalogService processCatalogService;
    
    @Lazy
    @Autowired
    private CycleService cycleService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessResponse createProcess(CreateProcessRequest request) {
        log.info("开始创建工序，循环ID: {}, 施工人员ID: {}", request.getCycleId(), request.getWorkerId());
        
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
        
        // 根据工序字典ID获取工序信息
        if (request.getProcessCatalogId() == null) {
            throw new BusinessException("工序字典ID不能为空");
        }
        
        ProcessCatalog catalog = processCatalogService.getById(request.getProcessCatalogId());
        if (catalog == null) {
            log.error("创建工序失败，工序字典不存在，工序字典ID: {}", request.getProcessCatalogId());
            throw new BusinessException("工序字典不存在，工序字典ID: " + request.getProcessCatalogId());
        }
        
        // 设置工序名称和字典ID
        process.setProcessName(catalog.getProcessName());
        process.setProcessCatalogId(request.getProcessCatalogId());
        // 通过工序字典创建的工序不需要模板ID
        process.setTemplateId(null);
        
        // 控制时长由用户输入
        if (request.getControlTime() == null) {
            throw new BusinessException("控制时长不能为空");
        }
        process.setControlTime(request.getControlTime());
        
        // 根据实际开始时间和控制时长标准自动计算预计结束时间
        if (request.getActualStartTime() != null && request.getControlTime() != null) {
            // 预计开始时间与实际开始时间一致
            process.setEstimatedStartTime(request.getActualStartTime());
            // 预计结束时间 = 实际开始时间 + 控制时长（分钟）
            process.setEstimatedEndTime(request.getActualStartTime().plusMinutes(request.getControlTime()));
            log.debug("自动计算预计结束时间，实际开始时间: {}, 控制时长: {}分钟, 预计结束时间: {}", 
                    request.getActualStartTime(), request.getControlTime(), process.getEstimatedEndTime());
        }
        
        log.debug("根据工序字典创建工序，工序字典ID: {}, 工序名称: {}, 控制时长: {}", 
                request.getProcessCatalogId(), catalog.getProcessName(), request.getControlTime());
        
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
        if (request.getOvertimeReason() != null) {
            process.setOvertimeReason(request.getOvertimeReason());
        }

        // 检测是否是"初喷后的测量放样"工序结束，如果是，则更新循环的实际里程并自动计算进尺
        boolean isMeasurementAfterSpray = isMeasurementAfterSprayProcess(process);
        boolean isCompleting = request.getStatus() != null && 
                ProcessStatus.COMPLETED.getCode().equals(request.getStatus());
        boolean wasCompleted = ProcessStatus.COMPLETED.getCode().equals(process.getProcessStatus());
        
        if (isMeasurementAfterSpray && (isCompleting || wasCompleted) && request.getActualMileage() != null) {
            updateCycleMileageAndCalculateAdvanceLength(process.getCycleId(), request.getActualMileage());
        }

        updateById(process);

        // 复用创建时的构造逻辑，返回最新数据
        ProcessResponse response = buildProcessResponse(process);

        log.info("更新工序成功，工序ID: {}", processId);
        return response;
    }
    
    /**
     * 判断是否是"初喷后的测量放样"工序
     * 
     * @param process 工序
     * @return 是否是初喷后的测量放样工序
     */
    private boolean isMeasurementAfterSprayProcess(Process process) {
        if (process == null || process.getProcessName() == null) {
            return false;
        }
        
        // 检查当前工序是否是"测量放样"
        boolean isMeasurement = process.getProcessName().contains("测量放样") || 
                process.getProcessName().equals("测量放样");
        
        if (!isMeasurement) {
            return false;
        }
        
        // 检查上一个工序是否是"初喷"
        if (process.getCycleId() != null && process.getStartOrder() != null) {
            Process previousProcess = getPreviousProcess(process.getCycleId(), process.getStartOrder());
            if (previousProcess != null && previousProcess.getProcessName() != null) {
                boolean isSpray = previousProcess.getProcessName().contains("初喷") || 
                        previousProcess.getProcessName().equals("初喷");
                if (isSpray) {
                    log.debug("检测到初喷后的测量放样工序，工序ID: {}, 工序名称: {}", 
                            process.getId(), process.getProcessName());
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 更新循环的实际里程并自动计算进尺长度
     * 进尺长度 = 本循环实际里程 - 上一循环实际里程（如果没有上一循环，则为0）
     * 
     * @param cycleId 循环ID
     * @param actualMileage 实际里程
     */
    private void updateCycleMileageAndCalculateAdvanceLength(Long cycleId, java.math.BigDecimal actualMileage) {
        if (cycleId == null || actualMileage == null) {
            log.warn("无法更新循环里程，循环ID或实际里程为空，循环ID: {}, 实际里程: {}", cycleId, actualMileage);
            return;
        }
        
        Cycle cycle = cycleMapper.selectById(cycleId);
        if (cycle == null) {
            log.warn("无法更新循环里程，循环不存在，循环ID: {}", cycleId);
            return;
        }
        
        // 更新循环的实际里程
        cycle.setActualMileage(actualMileage);
        
        // 计算进尺长度 = 本循环实际里程 - 上一循环实际里程
        java.math.BigDecimal baseMileage = java.math.BigDecimal.ZERO;
        
        // 获取上一循环
        if (cycle.getProjectId() != null && cycle.getCycleNumber() != null && cycle.getCycleNumber() > 1) {
            Cycle previousCycle = cycleService.getCycleByProjectAndNumber(
                    cycle.getProjectId(), cycle.getCycleNumber() - 1);
            if (previousCycle != null && previousCycle.getActualMileage() != null) {
                baseMileage = previousCycle.getActualMileage();
                log.debug("使用上一循环的实际里程作为基准，上一循环ID: {}, 循环号: {}, 实际里程: {}", 
                        previousCycle.getId(), previousCycle.getCycleNumber(), baseMileage);
            }
        }
        
        // 计算进尺长度
        java.math.BigDecimal advanceLength = actualMileage.subtract(baseMileage);
        if (advanceLength.compareTo(java.math.BigDecimal.ZERO) < 0) {
            log.warn("计算出的进尺长度为负数，循环ID: {}, 当前里程: {}, 基准里程: {}, 进尺: {}", 
                    cycle.getId(), actualMileage, baseMileage, advanceLength);
            advanceLength = java.math.BigDecimal.ZERO;
        }
        
        cycle.setAdvanceLength(advanceLength);
        cycleMapper.updateById(cycle);
        
        log.info("自动更新循环里程并计算进尺，循环ID: {}, 循环号: {}, 实际里程: {}, 基准里程: {}, 进尺: {} 米", 
                cycle.getId(), cycle.getCycleNumber(), actualMileage, baseMileage, advanceLength);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StartProcessResponse startWorkerProcess(Long processId, Long workerId, LocalDateTime actualStartTime) {
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

        // 检查上一工序是否完成，但不阻止开始，只返回提示
        Process previousProcess = null;
        StartProcessResponse response = new StartProcessResponse();
        response.setSuccess(true);
        
        if (process.getCycleId() != null && process.getStartOrder() != null) {
            previousProcess = getPreviousProcess(process.getCycleId(), process.getStartOrder());
            if (previousProcess != null && !ProcessStatus.COMPLETED.getCode().equals(previousProcess.getProcessStatus())) {
                // 上一工序未完成，返回提示信息
                response.setWarningMessage(String.format("上一工序'%s'尚未完成，请注意", previousProcess.getProcessName()));
                response.setPreviousProcessName(previousProcess.getProcessName());
                response.setPreviousProcessStatus(previousProcess.getProcessStatus());
                log.warn("开始工序时提示：上一工序未完成，工序ID: {}, 上一工序: {}", processId, previousProcess.getProcessName());
            }
        }

        process.setActualStartTime(actualStartTime);
        process.setProcessStatus(ProcessStatus.IN_PROGRESS.getCode());
        updateById(process);

        log.info("施工人员开始工序成功，工序ID: {}", processId);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessResponse completeWorkerProcess(Long processId, Long workerId) {
        log.info("施工人员完成工序，用户ID: {}, 工序ID: {}", workerId, processId);
        Process process = getById(processId);
        if (process == null) {
            throw new BusinessException(ResultCode.PROCESS_NOT_FOUND);
        }
        if (process.getOperatorId() == null || !process.getOperatorId().equals(workerId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (process.getActualStartTime() == null) {
            throw new BusinessException("当前工序未开始，无法完成");
        }
        if (ProcessStatus.COMPLETED.getCode().equals(process.getProcessStatus())) {
            return buildProcessResponse(process);
        }

        // 使用当前时间作为实际结束时间
        LocalDateTime now = LocalDateTime.now();
        process.setActualEndTime(now);
        process.setProcessStatus(ProcessStatus.COMPLETED.getCode());
        updateById(process);

        // 如果本循环所有工序都已完成，则将循环状态置为已完成并记录结束时间
        tryCompleteCycle(process);

        return buildProcessResponse(process);
    }

    /**
     * 当所在循环的所有工序都完成时，更新循环状态为已完成并填充结束时间
     */
    private void tryCompleteCycle(Process process) {
        if (process == null || process.getCycleId() == null) {
            return;
        }
        Long cycleId = process.getCycleId();

        // 判断是否还有未完成的工序
        Long unfinished = lambdaQuery()
                .eq(Process::getCycleId, cycleId)
                .ne(Process::getProcessStatus, ProcessStatus.COMPLETED.getCode())
                .count();
        if (unfinished != null && unfinished == 0) {
            Cycle cycle = cycleMapper.selectById(cycleId);
            if (cycle != null && !"COMPLETED".equals(cycle.getStatus())) {
                cycle.setStatus("COMPLETED");
                if (cycle.getEndDate() == null) {
                    cycle.setEndDate(LocalDateTime.now());
                }
                cycleMapper.updateById(cycle);
                log.info("循环所有工序完成，自动将循环置为已完成，循环ID: {}", cycleId);
            }
        }
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
        } else {
            // 如果没有上一工序，显示"无"
            response.setPreviousProcessName("无");
            response.setPreviousProcessStatus(null);
            response.setPreviousProcessStatusDesc("无");
        }

        // 处理时间相关字段
        if (process.getActualStartTime() != null && process.getActualEndTime() != null) {
            // 工序已完成：计算总耗时、超时/节时
            long minutes = Duration.between(process.getActualStartTime(), process.getActualEndTime()).toMinutes();
            int totalMinutes = (int) minutes;
            response.setProcessTimeMinutes(totalMinutes);

            // 计算超时或节时
            if (process.getControlTime() != null) {
                if (totalMinutes < process.getControlTime()) {
                    // 节时
                    int savedMinutes = process.getControlTime() - totalMinutes;
                    response.setTimeDifferenceText("节时" + savedMinutes + "分钟");
                } else if (totalMinutes > process.getControlTime()) {
                    // 超时
                    int overtimeMinutes = totalMinutes - process.getControlTime();
                    response.setTimeDifferenceText("超时" + overtimeMinutes + "分钟");
                } else {
                    // 正好等于控制时间
                    response.setTimeDifferenceText("按时完成");
                }
            } else {
                // 没有控制时间，无法计算超时/节时
                response.setTimeDifferenceText(null);
            }
        } else if (process.getActualStartTime() != null) {
            // 工序进行中：计算已进行时间（从实际开始时间到现在）
            long minutes = Duration.between(process.getActualStartTime(), LocalDateTime.now()).toMinutes();
            int elapsedMinutes = (int) minutes;
            response.setProcessTimeMinutes(elapsedMinutes);
            // 进行中的工序不设置 finalTime、savedTime、overtime
            // 设置已进行时间文本（>=60 分钟转换为小时+分钟展示）
            if (elapsedMinutes >= 60) {
                long hoursPart = elapsedMinutes / 60;
                long minutesPart = elapsedMinutes % 60;
                response.setTimeDifferenceText(minutesPart > 0
                        ? "已进行" + hoursPart + "小时" + minutesPart + "分钟"
                        : "已进行" + hoursPart + "小时");
            } else {
                response.setTimeDifferenceText("已进行" + elapsedMinutes + "分钟");
            }
        } else {
            // 工序未开始：processTimeMinutes 和 timeDifferenceText 都为 null
            response.setProcessTimeMinutes(null);
            response.setTimeDifferenceText(null);
        }
        
        // 设置超时原因
        response.setOvertimeReason(process.getOvertimeReason());

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
        response.setProcessCatalogId(process.getProcessCatalogId());
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

    @Override
    public com.zzw.zzwgx.dto.response.CycleProcessTimeResponse calculateCycleProcessTime(Long cycleId) {
        log.info("计算循环工序总时间，循环ID: {}", cycleId);
        Cycle cycle = cycleMapper.selectById(cycleId);
        if (cycle == null) {
            throw new BusinessException(ResultCode.CYCLE_NOT_FOUND);
        }

        List<Process> processes = getProcessesByCycleId(cycleId);
        
        com.zzw.zzwgx.dto.response.CycleProcessTimeResponse response = new com.zzw.zzwgx.dto.response.CycleProcessTimeResponse();
        response.setCycleId(cycleId);
        response.setCycleNumber(cycle.getCycleNumber());
        response.setTotalProcessCount(processes.size());

        // 计算单工序总时间（所有工序实际完成时间的总和，不考虑重叠）
        long totalIndividualTime = 0;
        int completedCount = 0;
        List<com.zzw.zzwgx.dto.response.CycleProcessTimeResponse.ProcessTimeDetail> details = new java.util.ArrayList<>();

        // 收集所有已完成工序的时间段
        List<TimeInterval> intervals = new java.util.ArrayList<>();
        
        for (Process process : processes) {
            com.zzw.zzwgx.dto.response.CycleProcessTimeResponse.ProcessTimeDetail detail = 
                new com.zzw.zzwgx.dto.response.CycleProcessTimeResponse.ProcessTimeDetail();
            detail.setProcessId(process.getId());
            detail.setProcessName(process.getProcessName());
            detail.setActualStartTime(process.getActualStartTime());
            detail.setActualEndTime(process.getActualEndTime());

            if (process.getActualStartTime() != null && process.getActualEndTime() != null) {
                long minutes = Duration.between(process.getActualStartTime(), process.getActualEndTime()).toMinutes();
                detail.setProcessTimeMinutes(minutes);
                totalIndividualTime += minutes;
                completedCount++;
                
                // 添加到时间段列表用于计算重叠
                intervals.add(new TimeInterval(process.getActualStartTime(), process.getActualEndTime()));
            } else {
                detail.setProcessTimeMinutes(0L);
            }
            details.add(detail);
        }

        response.setCompletedProcessCount(completedCount);
        response.setTotalIndividualTimeMinutes(totalIndividualTime);
        response.setProcessDetails(details);

        // 计算整套工序总时间（考虑重叠时间不重复计算）
        long totalCycleTime = calculateNonOverlappingTime(intervals);
        response.setTotalCycleTimeMinutes(totalCycleTime);
        response.setOverlapTimeMinutes(totalIndividualTime - totalCycleTime);

        log.info("计算循环工序总时间完成，循环ID: {}, 单工序总时间: {}分钟, 整套工序总时间: {}分钟, 重叠时间: {}分钟",
                cycleId, totalIndividualTime, totalCycleTime, totalIndividualTime - totalCycleTime);
        
        return response;
    }

    /**
     * 计算非重叠的总时间（合并重叠的时间段）
     */
    private long calculateNonOverlappingTime(List<TimeInterval> intervals) {
        if (intervals.isEmpty()) {
            return 0;
        }

        // 按开始时间排序
        intervals.sort((a, b) -> a.start.compareTo(b.start));

        // 合并重叠的时间段
        List<TimeInterval> merged = new java.util.ArrayList<>();
        TimeInterval current = intervals.get(0);

        for (int i = 1; i < intervals.size(); i++) {
            TimeInterval next = intervals.get(i);
            // 如果当前时间段与下一个时间段重叠或相邻，则合并
            if (!current.end.isBefore(next.start)) {
                // 重叠或相邻，合并
                if (current.end.isBefore(next.end)) {
                    current.end = next.end;
                }
            } else {
                // 不重叠，保存当前时间段，开始新的时间段
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);

        // 计算总时间
        long totalMinutes = 0;
        for (TimeInterval interval : merged) {
            totalMinutes += Duration.between(interval.start, interval.end).toMinutes();
        }

        return totalMinutes;
    }

    /**
     * 时间段内部类
     */
    private static class TimeInterval {
        LocalDateTime start;
        LocalDateTime end;

        TimeInterval(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitOvertimeReason(Long processId, Long workerId, String overtimeReason) {
        log.info("施工人员填报超时原因，用户ID: {}, 工序ID: {}, 超时原因: {}", workerId, processId, overtimeReason);
        Process process = getById(processId);
        if (process == null) {
            throw new BusinessException(ResultCode.PROCESS_NOT_FOUND);
        }
        
        // 验证权限：只有该工序的操作员可以填报
        if (process.getOperatorId() == null || !process.getOperatorId().equals(workerId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        
        // 验证工序是否已完成
        if (!ProcessStatus.COMPLETED.getCode().equals(process.getProcessStatus())) {
            throw new BusinessException("只有已完成的工序才能填报超时原因");
        }
        
        // 验证是否超时
        if (process.getActualStartTime() == null || process.getActualEndTime() == null || process.getControlTime() == null) {
            throw new BusinessException("工序时间信息不完整，无法判断是否超时");
        }
        long actualMinutes = Duration.between(process.getActualStartTime(), process.getActualEndTime()).toMinutes();
        if (actualMinutes <= process.getControlTime()) {
            throw new BusinessException("该工序未超时，无需填报超时原因");
        }
        
        // 验证循环是否已完成（只能在循环完成前填报）
        Cycle cycle = cycleMapper.selectById(process.getCycleId());
        if (cycle == null) {
            throw new BusinessException(ResultCode.CYCLE_NOT_FOUND);
        }
        if ("COMPLETED".equals(cycle.getStatus())) {
            throw new BusinessException("循环已完成，无法填报超时原因");
        }
        
        process.setOvertimeReason(overtimeReason);
        updateById(process);
        
        log.info("施工人员填报超时原因成功，工序ID: {}", processId);
    }

    @Override
    public Page<com.zzw.zzwgx.dto.response.OvertimeProcessResponse> getOvertimeProcessesWithoutReason(
            Integer pageNum, Integer pageSize, String projectName) {
        log.info("查询超时未填报原因的工序列表，页码: {}, 大小: {}, 工点名称: {}", pageNum, pageSize, projectName);
        
        // 查询所有已完成的工序，且循环未完成
        LambdaQueryWrapper<Process> processWrapper = new LambdaQueryWrapper<>();
        processWrapper.eq(Process::getProcessStatus, ProcessStatus.COMPLETED.getCode())
                .isNotNull(Process::getActualStartTime)
                .isNotNull(Process::getActualEndTime)
                .isNotNull(Process::getControlTime)
                .and(wrapper -> wrapper
                        .isNull(Process::getOvertimeReason)
                        .or()
                        .eq(Process::getOvertimeReason, ""))
                .orderByDesc(Process::getActualEndTime);
        
        Page<Process> processPage = page(new Page<>(pageNum, pageSize), processWrapper);
        
        if (processPage.getRecords().isEmpty()) {
            return new Page<>(pageNum, pageSize, 0);
        }
        
        // 获取循环信息
        List<Process> processes = processPage.getRecords();
        List<Long> cycleIds = processes.stream().map(Process::getCycleId).distinct().toList();
        List<Cycle> cycles = cycleIds.isEmpty() 
                ? java.util.Collections.emptyList()
                : cycleMapper.selectBatchIds(cycleIds);
        Map<Long, Cycle> cycleMap = cycles.stream()
                .filter(cycle -> !"COMPLETED".equals(cycle.getStatus())) // 只保留未完成的循环
                .collect(java.util.stream.Collectors.toMap(Cycle::getId, c -> c));
        
        // 获取项目信息
        List<Long> projectIds = cycles.stream().map(Cycle::getProjectId).distinct().toList();
        List<Project> projects = projectIds.isEmpty()
                ? java.util.Collections.emptyList()
                : projectMapper.selectBatchIds(projectIds);
        Map<Long, Project> projectMap = projects.stream()
                .collect(java.util.stream.Collectors.toMap(Project::getId, p -> p));
        
        // 获取操作员信息
        List<Long> operatorIds = processes.stream()
                .map(Process::getOperatorId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        List<User> operators = operatorIds.isEmpty()
                ? java.util.Collections.emptyList()
                : userService.listByIds(operatorIds);
        Map<Long, User> operatorMap = operators.stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));
        
        // 构建响应列表
        List<com.zzw.zzwgx.dto.response.OvertimeProcessResponse> result = new java.util.ArrayList<>();
        for (Process process : processes) {
            Cycle cycle = cycleMap.get(process.getCycleId());
            if (cycle == null) {
                continue; // 跳过已完成的循环
            }
            
            // 检查是否超时
            long actualMinutes = Duration.between(process.getActualStartTime(), process.getActualEndTime()).toMinutes();
            if (actualMinutes <= process.getControlTime()) {
                continue; // 跳过未超时的工序
            }
            
            Project project = projectMap.get(cycle.getProjectId());
            if (project == null) {
                continue;
            }
            
            // 工点名称过滤
            if (projectName != null && !projectName.isEmpty()) {
                if (project.getProjectName() == null || !project.getProjectName().contains(projectName)) {
                    continue;
                }
            }
            
            com.zzw.zzwgx.dto.response.OvertimeProcessResponse response = new com.zzw.zzwgx.dto.response.OvertimeProcessResponse();
            response.setProcessId(process.getId());
            response.setProcessName(process.getProcessName());
            response.setProjectName(project.getProjectName());
            response.setCycleNumber(cycle.getCycleNumber());
            response.setControlTime(process.getControlTime());
            response.setActualStartTime(process.getActualStartTime());
            response.setActualEndTime(process.getActualEndTime());
            response.setActualTimeMinutes(actualMinutes);
            response.setOvertimeMinutes(actualMinutes - process.getControlTime());
            
            if (process.getOperatorId() != null) {
                User operator = operatorMap.get(process.getOperatorId());
                if (operator != null) {
                    response.setOperatorName(operator.getRealName());
                }
            }
            
            result.add(response);
        }
        
        Page<com.zzw.zzwgx.dto.response.OvertimeProcessResponse> resultPage = new Page<>(pageNum, pageSize, result.size());
        resultPage.setRecords(result);
        
        log.info("查询超时未填报原因的工序列表完成，找到 {} 条记录", result.size());
        return resultPage;
    }
}

