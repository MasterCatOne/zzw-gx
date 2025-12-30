package com.zzw.zzwgx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzw.zzwgx.common.enums.ProcessStatus;
import com.zzw.zzwgx.common.enums.ResultCode;
import com.zzw.zzwgx.common.exception.BusinessException;
import com.zzw.zzwgx.dto.request.CreateProcessRequest;
import com.zzw.zzwgx.dto.request.FillProcessTimeRequest;
import com.zzw.zzwgx.dto.request.UpdateProcessOrderRequest;
import com.zzw.zzwgx.dto.request.UpdateProcessRequest;
import com.zzw.zzwgx.dto.response.*;
import com.zzw.zzwgx.entity.Cycle;
import com.zzw.zzwgx.entity.Process;
import com.zzw.zzwgx.entity.ProcessCatalog;
import com.zzw.zzwgx.entity.ProcessOperationLog;
import com.zzw.zzwgx.entity.Project;
import com.zzw.zzwgx.entity.User;
import com.zzw.zzwgx.mapper.CycleMapper;
import com.zzw.zzwgx.mapper.ProcessMapper;
import com.zzw.zzwgx.mapper.ProjectMapper;
import com.zzw.zzwgx.service.CycleService;
import com.zzw.zzwgx.service.ProcessCatalogService;
import com.zzw.zzwgx.service.ProcessOperationLogService;
import com.zzw.zzwgx.service.ProcessService;
import com.zzw.zzwgx.service.UserProjectService;
import com.zzw.zzwgx.service.UserService;
import com.zzw.zzwgx.security.SecurityUtils;
import org.springframework.util.CollectionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private final ProcessOperationLogService processOperationLogService;
    private final UserProjectService userProjectService;
    
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
        // 如果循环已完成，禁止再新增工序
        if ("COMPLETED".equals(cycle.getStatus())) {
            log.warn("创建工序失败，所属循环已完成，无法继续添加工序，循环ID: {}", request.getCycleId());
            throw new BusinessException("该循环已完成，不能再添加工序");
        }

        // 获取现有工序列表
        List<Process> existingProcesses = getProcessesByCycleId(request.getCycleId());
        
        // 检查是否要插入到进行中的工序之前
        // 规则：即使有进行中的工序，只要新工序的 startOrder > 进行中工序的 startOrder，就允许插入
        // 但不允许插入到进行中工序之前（startOrder <= 进行中工序的 startOrder）
        Process inProgressProcess = existingProcesses.stream()
                .filter(p -> ProcessStatus.IN_PROGRESS.getCode().equals(p.getProcessStatus()))
                .findFirst()
                .orElse(null);
        
        if (inProgressProcess != null && request.getStartOrder() <= inProgressProcess.getStartOrder()) {
            log.warn("创建工序失败，不能插入到进行中的工序之前，循环ID: {}, 新顺序: {}, 进行中工序顺序: {}", 
                    request.getCycleId(), request.getStartOrder(), inProgressProcess.getStartOrder());
            throw new BusinessException("不能将新工序插入到进行中的工序之前，进行中工序顺序: " + inProgressProcess.getStartOrder());
        }

        // 检查 startOrder 是否冲突，如果冲突则调整后续工序顺序
        boolean orderExists = existingProcesses.stream()
                .anyMatch(p -> Objects.equals(request.getStartOrder(), p.getStartOrder()));
        
        if (orderExists) {
            log.debug("工序顺序冲突，将调整后续工序顺序，循环ID: {}, 新顺序: {}", request.getCycleId(), request.getStartOrder());
            adjustProcessOrdersForInsert(request.getCycleId(), request.getStartOrder());
        }
        
        Process process = new Process();
        process.setCycleId(request.getCycleId());
        // 初始创建不填实际开始时间，保持未开始状态
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
        process.setCategory(catalog.getCategory());
        // 通过工序字典创建的工序不需要模板ID
        process.setTemplateId(null);
        
        // 控制时长由用户输入
        if (request.getControlTime() == null) {
            throw new BusinessException("控制时长不能为空");
        }
        process.setControlTime(request.getControlTime());
        
        // 根据预计开始时间和控制时长标准自动计算预计结束时间
        if (request.getEstimatedStartTime() != null && request.getControlTime() != null) {
            process.setEstimatedStartTime(request.getEstimatedStartTime());
            process.setEstimatedEndTime(request.getEstimatedStartTime().plusMinutes(request.getControlTime()));
            log.debug("自动计算预计结束时间，预计开始时间: {}, 控制时长: {}分钟, 预计结束时间: {}",
                    request.getEstimatedStartTime(), request.getControlTime(), process.getEstimatedEndTime());
        }
        
        log.debug("根据工序字典创建工序，工序字典ID: {}, 工序名称: {}, 控制时长: {}", 
                request.getProcessCatalogId(), catalog.getProcessName(), request.getControlTime());
        
        save(process);
        
        // 将工序的控制时间累加到循环的控制时间上
        if (request.getControlTime() != null && request.getControlTime() > 0) {
            Integer currentControlDuration = cycle.getControlDuration();
            if (currentControlDuration == null) {
                currentControlDuration = 0;
            }
            Integer newControlDuration = currentControlDuration + request.getControlTime();
            cycle.setControlDuration(newControlDuration);
            cycleMapper.updateById(cycle);
            log.info("更新循环控制时间，循环ID: {}, 原控制时间: {}分钟, 新增工序控制时间: {}分钟, 新控制时间: {}分钟",
                    request.getCycleId(), currentControlDuration, request.getControlTime(), newControlDuration);
        }
        
        log.info("工序创建成功，工序ID: {}, 工序名称: {}", process.getId(), process.getProcessName());

        return buildProcessResponse(process);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessResponse createProcessAndStart(CreateProcessRequest request) {
        log.info("创建并立即开工工序，循环ID: {}, 施工人员ID: {}", request.getCycleId(), request.getWorkerId());

        // 验证循环是否存在
        Cycle cycle = cycleMapper.selectById(request.getCycleId());
        if (cycle == null) {
            log.error("创建工序失败，循环不存在，循环ID: {}", request.getCycleId());
            throw new BusinessException(ResultCode.CYCLE_NOT_FOUND);
        }
        // 如果循环已完成，禁止再新增工序
        if ("COMPLETED".equals(cycle.getStatus())) {
            log.warn("创建工序失败，所属循环已完成，无法继续添加工序，循环ID: {}", request.getCycleId());
            throw new BusinessException("该循环已完成，不能再添加工序");
        }        // 检查是否已有进行中的工序
        List<Process> existingProcesses = getProcessesByCycleId(request.getCycleId());
        Process inProgressProcess = existingProcesses.stream()
                .filter(p -> ProcessStatus.IN_PROGRESS.getCode().equals(p.getProcessStatus()))
                .findFirst()
                .orElse(null);
        boolean hasInProgress = inProgressProcess != null;
        
        // 判断新建工序的顺序是否和进行中的工序顺序一致（startOrder相等）
        boolean sameOrderAsInProgress = hasInProgress
                && request.getStartOrder() != null
                && inProgressProcess.getStartOrder() != null
                && Objects.equals(request.getStartOrder(), inProgressProcess.getStartOrder());
        
        // 判断新建工序是否替换了进行中的工序（新建工序的startOrder <= 进行中工序的startOrder）
        boolean replaceInProgressProcess = hasInProgress
                && request.getStartOrder() != null
                && inProgressProcess.getStartOrder() != null
                && request.getStartOrder() <= inProgressProcess.getStartOrder();
        
        // 判断是否插入到进行中工序之后（新建工序的startOrder > 进行中工序的startOrder）
        boolean insertAfterInProgress = hasInProgress
                && request.getStartOrder() != null
                && inProgressProcess.getStartOrder() != null
                && request.getStartOrder() > inProgressProcess.getStartOrder();

        // 如果没有进行中的工序，则要求实际开始时间
        if (!hasInProgress && request.getActualStartTime() == null) {
            throw new BusinessException("实际开始时间不能为空");
        }
        
        // 验证实际开始时间不能是过去时间（允许3分钟内的误差）
        // 注意：如果替换进行中的工序，实际开始时间会被忽略，使用原工序的时间
        if (request.getActualStartTime() != null && !replaceInProgressProcess) {
            LocalDateTime now = LocalDateTime.now();
            if (request.getActualStartTime().isBefore(now.minusMinutes(3))) {
                log.error("创建工序失败，实际开始时间不能是过去时间（超过3分钟误差），实际开始时间: {}, 当前时间: {}", 
                        request.getActualStartTime(), now);
                throw new BusinessException(ResultCode.PROCESS_START_TIME_INVALID);
            }
        }

        // 验证新工序不能插入到已完成的工序前面
        if (request.getStartOrder() != null) {
            Integer maxCompletedOrder = existingProcesses.stream()
                    .filter(p -> ProcessStatus.COMPLETED.getCode().equals(p.getProcessStatus()))
                    .filter(p -> p.getStartOrder() != null)
                    .map(Process::getStartOrder)
                    .max(Integer::compareTo)
                    .orElse(null);
            
            if (maxCompletedOrder != null && request.getStartOrder() <= maxCompletedOrder) {
                log.error("创建工序失败，不能插入到已完成的工序前面，新工序顺序: {}, 最大已完成工序顺序: {}", 
                        request.getStartOrder(), maxCompletedOrder);
                throw new BusinessException("不能将工序插入到已完成的工序前面，当前最大已完成工序顺序为: " + maxCompletedOrder);
            }
        }

        // 处理 startOrder 冲突（调整后续工序顺序）
        boolean orderExists = existingProcesses.stream()
                .anyMatch(p -> Objects.equals(request.getStartOrder(), p.getStartOrder()));
        
        if (orderExists) {
            log.debug("工序顺序冲突，将调整后续工序顺序，循环ID: {}, 新顺序: {}", request.getCycleId(), request.getStartOrder());
            adjustProcessOrdersForInsert(request.getCycleId(), request.getStartOrder());
        }

        // 如果新建工序的顺序和进行中的工序顺序一致，先清除原工序的时间字段，并保存时间供新工序使用
        LocalDateTime inheritedEstimatedStartTime = null;
        LocalDateTime inheritedActualStartTime = null;
        if (sameOrderAsInProgress) {
            Process current = getById(inProgressProcess.getId());
            if (current != null) {
                // 保存原工序的预计开始时间和实际开始时间，供新工序继承使用
                inheritedEstimatedStartTime = current.getEstimatedStartTime();
                inheritedActualStartTime = current.getActualStartTime();
                
                log.info("准备清除原工序时间字段，工序ID: {}, 原预计开始时间: {}, 原实际开始时间: {}, 原预计结束时间: {}",
                        current.getId(), current.getEstimatedStartTime(), current.getActualStartTime(), current.getEstimatedEndTime());
                
                // 使用 UpdateWrapper 确保 null 值被正确更新到数据库
                LambdaUpdateWrapper<Process> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(Process::getId, current.getId())
                        .set(Process::getEstimatedStartTime, null)
                        .set(Process::getActualStartTime, null)
                        .set(Process::getEstimatedEndTime, null)
                        .set(Process::getActualEndTime, null)
                        .set(Process::getProcessStatus, ProcessStatus.NOT_STARTED.getCode());
                boolean updateResult = update(updateWrapper);
                
                log.info("清除原工序时间字段完成，工序ID: {}, 更新结果: {}", current.getId(), updateResult);
                
                logProcessOperation(current.getId(), request.getWorkerId(), "RESET_IN_PROGRESS",
                        "新建工序顺序与进行中工序一致，原进行中工序时间字段已清除，预计开始时间和实际开始时间将继承给新工序");
            }
        } else if (replaceInProgressProcess) {
            // 如果需要替换进行中的工序（但顺序不一致），先将其状态重置并清空时间字段
            Process current = getById(inProgressProcess.getId());
            if (current != null) {
                log.info("准备清除原工序时间字段（替换情况），工序ID: {}, 原预计开始时间: {}, 原实际开始时间: {}, 原预计结束时间: {}",
                        current.getId(), current.getEstimatedStartTime(), current.getActualStartTime(), current.getEstimatedEndTime());
                
                // 使用 UpdateWrapper 确保 null 值被正确更新到数据库
                LambdaUpdateWrapper<Process> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(Process::getId, current.getId())
                        .set(Process::getEstimatedStartTime, null)
                        .set(Process::getActualStartTime, null)
                        .set(Process::getEstimatedEndTime, null)
                        .set(Process::getActualEndTime, null)
                        .set(Process::getProcessStatus, ProcessStatus.NOT_STARTED.getCode());
                boolean updateResult = update(updateWrapper);
                
                log.info("清除原工序时间字段完成（替换情况），工序ID: {}, 更新结果: {}", current.getId(), updateResult);
                
                logProcessOperation(current.getId(), request.getWorkerId(), "RESET_IN_PROGRESS",
                        "插入新工序替换进行中工序，原进行中工序重置为未开始并清空时间字段");
            }
        }

        Process process = new Process();
        process.setCycleId(request.getCycleId());
        
        // 根据是否有进行中的工序决定新工序的状态
        if (hasInProgress) {
            if (sameOrderAsInProgress || replaceInProgressProcess) {
                // 顺序一致或替换进行中的工序，新工序设置为进行中状态
                process.setProcessStatus(ProcessStatus.IN_PROGRESS.getCode());
            } else {
                // 插入到进行中工序之后，新工序设置为未开始状态
                process.setProcessStatus(ProcessStatus.NOT_STARTED.getCode());
                log.debug("循环已有进行中的工序，新工序将创建为未开始状态，循环ID: {}", request.getCycleId());
            }
        } else {
            // 如果没有进行中的工序，新工序为进行中状态
            process.setProcessStatus(ProcessStatus.IN_PROGRESS.getCode());
        }
        
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

        process.setProcessName(catalog.getProcessName());
        process.setProcessCatalogId(request.getProcessCatalogId());
        process.setCategory(catalog.getCategory());
        process.setTemplateId(null);

        if (request.getControlTime() == null) {
            throw new BusinessException("控制时长不能为空");
        }
        process.setControlTime(request.getControlTime());

        // 根据情况设置时间字段
        if (sameOrderAsInProgress) {
            // 情况1：新建工序顺序和进行中工序顺序一致
            // 继承原工序的预计开始时间和实际开始时间，根据新工序的控制时间计算预计结束时间
            process.setActualStartTime(inheritedActualStartTime);
            process.setEstimatedStartTime(inheritedEstimatedStartTime);
            if (inheritedEstimatedStartTime != null && request.getControlTime() != null) {
                process.setEstimatedEndTime(inheritedEstimatedStartTime.plusMinutes(request.getControlTime()));
                log.debug("新建工序顺序与进行中工序一致，继承实际开始时间: {}, 继承预计开始时间: {}, 根据控制时间计算预计结束时间: {}",
                        inheritedActualStartTime, inheritedEstimatedStartTime, process.getEstimatedEndTime());
            } else {
                process.setEstimatedEndTime(null);
                log.debug("新建工序顺序与进行中工序一致，但原工序预计开始时间为空或控制时间为空，不设置预计结束时间");
            }
        } else if (replaceInProgressProcess) {
            // 情况2：替换进行中的工序（顺序不一致但startOrder <= 进行中工序的startOrder），使用原进行中工序的时间
            process.setActualStartTime(inProgressProcess.getActualStartTime());
            process.setEstimatedStartTime(inProgressProcess.getEstimatedStartTime());
            process.setEstimatedEndTime(inProgressProcess.getEstimatedEndTime());
            log.debug("替换进行中的工序，使用原工序的时间，实际开始时间: {}, 预计开始时间: {}, 预计结束时间: {}",
                    process.getActualStartTime(), process.getEstimatedStartTime(), process.getEstimatedEndTime());
        } else if (insertAfterInProgress) {
            // 情况2：插入到进行中工序之后，不设置任何时间字段
            process.setActualStartTime(null);
            process.setEstimatedStartTime(null);
            process.setEstimatedEndTime(null);
            log.debug("插入到进行中工序之后，不设置时间字段，循环ID: {}", request.getCycleId());
        } else if (!hasInProgress) {
            // 情况3：没有进行中的工序，新工序为进行中状态，设置实际开始时间
            process.setActualStartTime(request.getActualStartTime());
            // 预计开始时间为空时，默认等于实际开始时间
            LocalDateTime estimatedStart = request.getEstimatedStartTime() != null
                    ? request.getEstimatedStartTime()
                    : request.getActualStartTime();
            process.setEstimatedStartTime(estimatedStart);
            // 计算预计结束时间
            if (process.getEstimatedStartTime() != null && request.getControlTime() != null) {
                process.setEstimatedEndTime(process.getEstimatedStartTime().plusMinutes(request.getControlTime()));
                log.debug("自动计算预计结束时间，预计开始时间: {}, 控制时长: {}分钟, 预计结束时间: {}",
                        process.getEstimatedStartTime(), request.getControlTime(), process.getEstimatedEndTime());
            }
        }

        save(process);
        
        // 将工序的控制时间累加到循环的控制时间上
        if (request.getControlTime() != null && request.getControlTime() > 0) {
            Integer currentControlDuration = cycle.getControlDuration();
            if (currentControlDuration == null) {
                currentControlDuration = 0;
            }
            Integer newControlDuration = currentControlDuration + request.getControlTime();
            cycle.setControlDuration(newControlDuration);
            cycleMapper.updateById(cycle);
            log.info("更新循环控制时间，循环ID: {}, 原控制时间: {}分钟, 新增工序控制时间: {}分钟, 新控制时间: {}分钟",
                    request.getCycleId(), currentControlDuration, request.getControlTime(), newControlDuration);
        }
        
        if (hasInProgress) {
            if (sameOrderAsInProgress) {
                log.info("工序创建成功，顺序与进行中工序一致，原工序时间已清除，新工序ID: {}, 工序名称: {}", 
                        process.getId(), process.getProcessName());
                logProcessOperation(process.getId(), request.getWorkerId(), "REPLACE_AND_START", null);
            } else if (replaceInProgressProcess) {
                log.info("工序创建成功，替换当前进行中的工序为新工序，工序ID: {}, 工序名称: {}", 
                        process.getId(), process.getProcessName());
                logProcessOperation(process.getId(), request.getWorkerId(), "REPLACE_AND_START", null);
            } else {
                log.info("工序创建成功（插入到进行中工序之后，新工序为未开始状态），工序ID: {}, 工序名称: {}", 
                        process.getId(), process.getProcessName());
                logProcessOperation(process.getId(), request.getWorkerId(), "CREATE", null);
            }
        } else {
            log.info("工序创建并开工成功，工序ID: {}, 工序名称: {}", process.getId(), process.getProcessName());
            logProcessOperation(process.getId(), request.getWorkerId(), "CREATE_AND_START", null);
        }
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
//        if (process.getOperatorId() == null || !process.getOperatorId().equals(workerId)) {
//            log.warn("没有权限查看该工序详情，工序ID: {}, 用户ID: {}", processId, workerId);
//            throw new BusinessException(ResultCode.FORBIDDEN);
//        }
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
                ? Collections.emptyList()
                : cycleMapper.selectBatchIds(cycleIds);
        Map<Long, Cycle> cycleMap = cycles.stream().collect(Collectors.toMap(Cycle::getId, c -> c));

        List<Long> projectIds = cycles.stream().map(Cycle::getProjectId).distinct().toList();
        List<Project> projects = projectIds.isEmpty()
                ? Collections.emptyList()
                : projectMapper.selectBatchIds(projectIds);
        Map<Long, Project> projectMap = projects.stream().collect(Collectors.toMap(Project::getId, p -> p));

        // 映射为响应DTO，并在内存中根据项目名称做过滤
        List<WorkerProcessListResponse> all = new ArrayList<>();
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
            result.setRecords(Collections.emptyList());
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
            // 检查工序是否已完成，已完成的工序不能修改开始顺序
            if (ProcessStatus.COMPLETED.getCode().equals(process.getProcessStatus())) {
                log.error("更新工序失败，已完成的工序不能修改开始顺序，工序ID: {}", processId);
                throw new BusinessException(ResultCode.PROCESS_COMPLETED_CANNOT_UPDATE_ORDER);
            }
            
            // 如果新顺序与当前顺序不同，检查是否有其他未开始工序占用该顺序
            if (!Objects.equals(request.getStartOrder(), process.getStartOrder())) {
                List<Process> allProcesses = getProcessesByCycleId(process.getCycleId());
                Process targetProcess = allProcesses.stream()
                        .filter(p -> Objects.equals(p.getStartOrder(), request.getStartOrder())
                                && !p.getId().equals(processId))
                        .findFirst()
                        .orElse(null);
                
                if (targetProcess != null) {
                    // 如果目标顺序被另一个未开始工序占用，则交换两个工序的顺序
                    if (ProcessStatus.NOT_STARTED.getCode().equals(targetProcess.getProcessStatus())) {
                        Integer tempOrder = process.getStartOrder();
                        process.setStartOrder(request.getStartOrder());
                        targetProcess.setStartOrder(tempOrder);
                        updateById(targetProcess);
                        log.info("交换工序顺序，工序ID: {} 和工序ID: {} 交换顺序", processId, targetProcess.getId());
                    } else {
                        // 如果目标顺序被已完成或进行中的工序占用，抛出异常
                        log.error("更新工序失败，目标顺序已被{}工序占用，工序ID: {}", 
                                targetProcess.getProcessStatus(), targetProcess.getId());
                        throw new BusinessException("目标顺序已被其他工序占用，无法修改");
                    }
                } else {
                    // 目标顺序未被占用，直接设置
                    process.setStartOrder(request.getStartOrder());
                }
            }
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

        // 记录开始操作
        logProcessOperation(processId, workerId, "START", null);

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
        
        // 检查权限：用户必须是有该工点权限的人，或者是系统管理员
        if (!hasPermissionToCompleteProcess(process, workerId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "您没有该工点的权限，无法完成此工序");
        }
        
        // 验证工序状态：只能完成未完成的工序（NOT_STARTED 或 IN_PROGRESS）
        if (ProcessStatus.COMPLETED.getCode().equals(process.getProcessStatus())) {
            log.warn("尝试完成已完成的工序，用户ID: {}, 工序ID: {}", workerId, processId);
            throw new BusinessException("该工序已完成，无法再次完成");
        }
        
        // 设置操作员ID为完成工序的人（无论之前是否有操作员）
        process.setOperatorId(workerId);
        log.info("设置工序操作员，工序ID: {}, 操作员ID: {}", processId, workerId);
        if (process.getActualStartTime() == null) {
            throw new BusinessException("当前工序未开始，无法完成");
        }

        // 使用当前时间作为实际结束时间
        LocalDateTime now = LocalDateTime.now();
        process.setActualEndTime(now);
        process.setProcessStatus(ProcessStatus.COMPLETED.getCode());
        updateById(process);

        logProcessOperation(processId, workerId, "COMPLETED", null);

        // 完成后自动开启下一道未开始的工序
        // 使用当前工序的结束时间作为下一个工序的开始时间
        startNextProcess(process, workerId, process.getActualEndTime());

        // 如果本循环所有工序都已完成，则将循环状态置为已完成并记录结束时间
        tryCompleteCycle(process);
        return buildProcessResponse(process);
    }
    
    @Override
    public ProcessResponse completeWorkerProcessAndCheckCycle(Long processId, Long workerId) {
        log.info("施工人员完成工序并检查循环状态，用户ID: {}, 工序ID: {}", workerId, processId);
        Process process = getById(processId);
        if (process == null) {
            throw new BusinessException(ResultCode.PROCESS_NOT_FOUND);
        }
        
        // 检查权限：用户必须是有该工点权限的人，或者是系统管理员
        if (!hasPermissionToCompleteProcess(process, workerId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "您没有该工点的权限，无法完成此工序");
        }
        
        // 验证工序状态：只能完成未完成的工序（NOT_STARTED 或 IN_PROGRESS）
        if (ProcessStatus.COMPLETED.getCode().equals(process.getProcessStatus())) {
            log.warn("尝试完成已完成的工序，用户ID: {}, 工序ID: {}", workerId, processId);
            throw new BusinessException("该工序已完成，无法再次完成");
        }
        
        // 设置操作员ID为完成工序的人（无论之前是否有操作员）
        process.setOperatorId(workerId);
        log.info("设置工序操作员，工序ID: {}, 操作员ID: {}", processId, workerId);
        if (process.getActualStartTime() == null) {
            throw new BusinessException("当前工序未开始，无法完成");
        }

        // 使用当前时间作为实际结束时间
        LocalDateTime now = LocalDateTime.now();
        process.setActualEndTime(now);
        process.setProcessStatus(ProcessStatus.COMPLETED.getCode());
        updateById(process);

        logProcessOperation(processId, workerId, "COMPLETED_AND_NEXT", null);

        // 完成后自动开启下一道未开始的工序
        // 使用当前工序的结束时间作为下一个工序的开始时间
        startNextProcess(process, workerId, process.getActualEndTime());

        // 如果本循环所有工序都已完成，则将循环状态置为已完成并记录结束时间
        tryCompleteCycle(process);

        return buildProcessResponse(process);
    }
    
    // 授权方法已废弃，超过24小时的工序只能由系统管理员直接补填，无需授权步骤
    // @Override
    // @Transactional(rollbackFor = Exception.class)
    // public void authorizeTimeFill(Long processId) {
    //     log.info("系统管理员授权工序时间补填，工序ID: {}", processId);
    //     
    //     // 检查当前用户是否是系统管理员
    //     var currentUser = SecurityUtils.getCurrentUser();
    //     if (currentUser == null) {
    //         log.error("授权失败，未获取到当前登录用户");
    //         throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "未获取到当前登录用户");
    //     }
    //     
    //     var roles = currentUser.getRoleCodes();
    //     boolean isSystemAdmin = roles.stream().anyMatch(r -> "SYSTEM_ADMIN".equals(r));
    //     if (!isSystemAdmin) {
    //         log.error("授权失败，当前用户不是系统管理员，用户ID: {}", currentUser.getUserId());
    //         throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "只有系统管理员可以授权时间补填");
    //     }
    //     
    //     // 验证工序是否存在
    //     Process process = getById(processId);
    //     if (process == null) {
    //         log.error("授权失败，工序不存在，工序ID: {}", processId);
    //         throw new BusinessException(ResultCode.PROCESS_NOT_FOUND);
    //     }
    //     
    //     // 设置授权标志
    //     process.setTimeFillAuthorized(1);
    //     updateById(process);
    //     
    //     // 记录操作日志
    //     logProcessOperation(processId, currentUser.getUserId(), "AUTHORIZE_TIME_FILL", null);
    //     
    //     log.info("系统管理员授权工序时间补填成功，工序ID: {}, 授权人ID: {}", processId, currentUser.getUserId());
    // }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessResponse fillProcessTime(Long processId, Long workerId, FillProcessTimeRequest request) {
        log.info("施工人员补填工序时间，用户ID: {}, 工序ID: {}, 实际开始时间: {}, 实际结束时间: {}", 
                workerId, processId, request.getActualStartTime(), request.getActualEndTime());
        
        // 验证工序是否存在
        Process process = getById(processId);
        if (process == null) {
            throw new BusinessException(ResultCode.PROCESS_NOT_FOUND);
        }
        
        // 检查权限：用户必须是有该工点权限的人，或者是系统管理员
        if (!hasPermissionToCompleteProcess(process, workerId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "您没有该工点的权限，无法补填此工序时间");
        }
        
//        // 验证工序状态：只能补填已完成的工序
//        if (!ProcessStatus.COMPLETED.getCode().equals(process.getProcessStatus())) {
//            log.warn("尝试补填未完成的工序，用户ID: {}, 工序ID: {}, 工序状态: {}", workerId, processId, process.getProcessStatus());
//            throw new BusinessException("只能补填已完成的工序。进行中的工序请先完成，未开始的工序请先开始。");
//        }
        
        // 如果用户没有提供时间，使用预计时间作为默认值
        if (request.getActualStartTime() == null) {
            if (process.getEstimatedStartTime() == null) {
                throw new BusinessException("工序没有预计开始时间，请手动填写实际开始时间");
            }
            request.setActualStartTime(process.getEstimatedStartTime());
            log.debug("用户未提供实际开始时间，使用预计开始时间: {}", process.getEstimatedStartTime());
        }
        
        if (request.getActualEndTime() == null) {
            if (process.getEstimatedEndTime() == null) {
                throw new BusinessException("工序没有预计结束时间，请手动填写实际结束时间");
            }
            request.setActualEndTime(process.getEstimatedEndTime());
            log.debug("用户未提供实际结束时间，使用预计结束时间: {}", process.getEstimatedEndTime());
        }
        
        // 对于已完成工序，重新计算正确的预计结束时间（基于预计开始时间+控制时间）
        LocalDateTime correctEstimatedEndTime = process.getEstimatedEndTime();
        if (ProcessStatus.COMPLETED.getCode().equals(process.getProcessStatus()) 
                && process.getEstimatedStartTime() != null 
                && process.getControlTime() != null) {
            correctEstimatedEndTime = process.getEstimatedStartTime().plusMinutes(process.getControlTime());
            log.debug("已完成工序重新计算预计结束时间，工序ID: {}, 预计开始时间: {}, 控制时间: {}分钟, 计算后的预计结束时间: {}", 
                    processId, process.getEstimatedStartTime(), process.getControlTime(), correctEstimatedEndTime);
        }
        
        // 验证时间合理性
        if (request.getActualStartTime() != null && request.getActualEndTime() != null) {
            if (!request.getActualStartTime().isBefore(request.getActualEndTime())) {
                throw new BusinessException("实际开始时间必须早于实际结束时间");
            }
        }
        
        // 验证实际开始时间不能早于预计开始时间（对所有状态的工序都生效）
        if (request.getActualStartTime() != null && process.getEstimatedStartTime() != null) {
            if (request.getActualStartTime().isBefore(process.getEstimatedStartTime())) {
                log.error("补填失败，实际开始时间早于预计开始时间，工序ID: {}, 实际开始时间: {}, 预计开始时间: {}", 
                        processId, request.getActualStartTime(), process.getEstimatedStartTime());
                throw new BusinessException("实际开始时间不能早于预计开始时间");
            }
        }
        
        // 已移除实际结束时间不能晚于预计结束时间的限制，允许在预计结束时间之外补填
        
        // 计算从预计结束时间到当前时间的小时数
        // 如果超过24小时，只能由系统管理员补填
        // 对于已完成工序，使用重新计算的预计结束时间
        LocalDateTime now = LocalDateTime.now();
        boolean exceeds24Hours = false;
        LocalDateTime estimatedEndTimeFor24HourCheck = correctEstimatedEndTime != null 
                ? correctEstimatedEndTime 
                : process.getEstimatedEndTime();
        if (estimatedEndTimeFor24HourCheck != null) {
            long hoursSinceEstimatedEnd = Duration.between(estimatedEndTimeFor24HourCheck, now).toHours();
            exceeds24Hours = hoursSinceEstimatedEnd > 24;
            log.debug("工序预计结束时间: {}, 当前时间: {}, 时间差: {}小时, 超过24小时: {}", 
                    estimatedEndTimeFor24HourCheck, now, hoursSinceEstimatedEnd, exceeds24Hours);
        } else {
            log.warn("工序没有预计结束时间，无法判断是否超过24小时，工序ID: {}", processId);
        }
        
        // 如果超过24小时，检查是否是系统管理员
        if (exceeds24Hours) {
            var currentUser = SecurityUtils.getCurrentUser();
            if (currentUser == null) {
                log.error("补填失败，超过24小时且未获取到当前登录用户，工序ID: {}", processId);
                throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "未获取到当前登录用户");
            }
            var roles = currentUser.getRoleCodes();
            boolean isSystemAdmin = roles.stream().anyMatch(r -> "SYSTEM_ADMIN".equals(r));
            if (!isSystemAdmin) {
                log.error("补填失败，超过24小时且不是系统管理员，工序ID: {}, 用户ID: {}, 预计结束时间: {}", 
                        processId, workerId, estimatedEndTimeFor24HourCheck);
                throw new BusinessException("超过24小时的工序只能由系统管理员补填，请联系管理员");
            }
            log.info("超过24小时，系统管理员补填，工序ID: {}, 管理员ID: {}", processId, workerId);
        }
        
        // 更新实际开始时间和实际结束时间
        if (request.getActualStartTime() != null) {
            process.setActualStartTime(request.getActualStartTime());
        }
        if (request.getActualEndTime() != null) {
            process.setActualEndTime(request.getActualEndTime());
        }
        
        // 对于已完成工序，确保数据库中的预计结束时间是正确的（基于预计开始时间+控制时间）
        if (ProcessStatus.COMPLETED.getCode().equals(process.getProcessStatus()) 
                && process.getEstimatedStartTime() != null 
                && process.getControlTime() != null) {
            LocalDateTime correctEstimatedEndTimeForDB = process.getEstimatedStartTime().plusMinutes(process.getControlTime());
            process.setEstimatedEndTime(correctEstimatedEndTimeForDB);
            log.debug("更新已完成工序的预计结束时间到数据库，工序ID: {}, 预计结束时间: {}", 
                    processId, correctEstimatedEndTimeForDB);
        }
        
        // 判断工序是否已完成
        boolean wasCompleted = ProcessStatus.COMPLETED.getCode().equals(process.getProcessStatus());
        
        // 设置操作员ID：
        // - 如果工序已有操作人员，不覆盖原有操作人员（管理员补填时保留原操作人员）
        // - 如果工序没有操作人员，设置为当前用户
        if (process.getOperatorId() != null) {
            log.debug("工序已有操作人员，不覆盖操作人员，工序ID: {}, 原操作人员ID: {}, 当前用户ID: {}", 
                    processId, process.getOperatorId(), workerId);
        } else {
            process.setOperatorId(workerId);
            log.debug("设置操作员ID为当前用户，工序ID: {}, 操作员ID: {}", processId, workerId);
        }
        
        // 如果工序状态不是已完成，更新为已完成
        if (!wasCompleted) {
            process.setProcessStatus(ProcessStatus.COMPLETED.getCode());
            log.info("补填时间后自动将工序状态更新为已完成，工序ID: {}", processId);
        }
        
        updateById(process);
        
        // 记录操作日志
        logProcessOperation(processId, workerId, "FILL_TIME", 
                String.format("补填时间：开始时间=%s, 结束时间=%s", 
                        request.getActualStartTime(), request.getActualEndTime()));
        
        // 根据工序之前的状态决定后续处理：
        // - 如果补填的是已完成的工序：不更新后续工序的时间
        // - 如果补填的是进行中的工序：开启下一个工序，但不更新后续工序的时间
        if (!wasCompleted) {
            // 工序之前是进行中：补填后自动开启下一道未开始的工序
            // 使用补填的结束时间作为下一个工序的开始时间
            // 使用当前工序的操作人员（如果工序原本有操作人员，则保留；如果没有，则使用补填人员）
            // 这样即使管理员补填，下一个工序的操作人员仍然是原操作人员，而不是管理员
            Long nextProcessOperatorId = process.getOperatorId();
            startNextProcess(process, nextProcessOperatorId, request.getActualEndTime());
            
            // 如果本循环所有工序都已完成，则将循环状态置为已完成并记录结束时间
            tryCompleteCycle(process);
        }
        // 如果工序之前已完成，不更新后续工序的时间，也不开启下一工序
        
        log.info("施工人员补填工序时间成功，工序ID: {}, 用户ID: {}", processId, workerId);
        return buildProcessResponse(process);
    }

    /**
     * 检查用户是否有权限完成该工序
     * 规则：用户必须是系统管理员，或者有该工序所属工点的权限（包括父节点权限）
     * 
     * @param process 工序对象
     * @param userId 用户ID
     * @return true表示有权限，false表示无权限
     */
    private boolean hasPermissionToCompleteProcess(Process process, Long userId) {
        if (process == null || process.getCycleId() == null || userId == null) {
            return false;
        }
        
        // 获取当前用户信息
        var currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            log.warn("未获取到当前登录用户，用户ID: {}", userId);
            return false;
        }
        
        // 检查是否是系统管理员
        var roles = currentUser.getRoleCodes();
        boolean isSystemAdmin = roles.stream().anyMatch(r -> "SYSTEM_ADMIN".equals(r));
        if (isSystemAdmin) {
            log.debug("系统管理员有权限完成所有工序，用户ID: {}", userId);
            return true;
        }
        
        // 获取工序所属的循环
        Cycle cycle = cycleMapper.selectById(process.getCycleId());
        if (cycle == null || cycle.getProjectId() == null) {
            log.warn("无法获取工序所属的工点，工序ID: {}, 循环ID: {}", process.getId(), process.getCycleId());
            return false;
        }
        
        Long projectId = cycle.getProjectId();
        
        // 获取用户有权限的节点ID列表（可能包括父节点）
        List<Long> allowedProjectIds = userProjectService.getProjectIdsByUser(userId);
        if (CollectionUtils.isEmpty(allowedProjectIds)) {
            log.debug("用户未分配任何工点权限，用户ID: {}", userId);
            return false;
        }
        
        // 检查用户是否有该工点的权限
        // 1. 直接检查工点ID是否在分配列表中
        if (allowedProjectIds.contains(projectId)) {
            log.debug("用户直接分配了该工点，用户ID: {}, 工点ID: {}", userId, projectId);
            return true;
        }
        
        // 2. 检查工点的所有父节点是否在分配列表中
        // 如果用户被分配到父节点（如标段、隧道），则自动包含其下所有子工点
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            log.warn("工点不存在，工点ID: {}", projectId);
            return false;
        }
        
        // 向上遍历父节点，检查是否有任何一个父节点在用户的分配列表中
        Long currentParentId = project.getParentId();
        while (currentParentId != null) {
            if (allowedProjectIds.contains(currentParentId)) {
                log.debug("用户通过父节点拥有该工点权限，用户ID: {}, 工点ID: {}, 父节点ID: {}", 
                        userId, projectId, currentParentId);
                return true;
            }
            Project parentProject = projectMapper.selectById(currentParentId);
            if (parentProject == null) {
                break;
            }
            currentParentId = parentProject.getParentId();
        }
        
        log.warn("用户没有该工点的权限，用户ID: {}, 工点ID: {}, 用户有权限的节点: {}", 
                userId, projectId, allowedProjectIds);
        return false;
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

    /**
     * 完成当前工序后，自动开启下一道未开始的工序
     * @param currentProcess 当前工序
     * @param workerId 操作员ID
     * @param suggestedStartTime 建议的开始时间（如果提供，则使用此时间；否则使用当前时间）
     */
    private void startNextProcess(Process currentProcess, Long workerId, LocalDateTime suggestedStartTime) {
        if (currentProcess == null || currentProcess.getCycleId() == null || currentProcess.getStartOrder() == null) {
            return;
        }
        Process next = lambdaQuery()
                .eq(Process::getCycleId, currentProcess.getCycleId())
                .gt(Process::getStartOrder, currentProcess.getStartOrder())
                .eq(Process::getProcessStatus, ProcessStatus.NOT_STARTED.getCode())
                .orderByAsc(Process::getStartOrder)
                .last("LIMIT 1")
                .one();
        if (next == null) {
            return;
        }
        // 如果提供了建议的开始时间（通常是补填工序的结束时间），使用该时间；否则使用当前时间
        LocalDateTime startTime = suggestedStartTime != null ? suggestedStartTime : LocalDateTime.now();
        next.setProcessStatus(ProcessStatus.IN_PROGRESS.getCode());
        next.setActualStartTime(startTime);
        next.setEstimatedStartTime(startTime);
        if (next.getControlTime() != null) {
            next.setEstimatedEndTime(startTime.plusMinutes(next.getControlTime()));
        }
        if (next.getOperatorId() == null && workerId != null) {
            next.setOperatorId(workerId);
        }
        updateById(next);
        // 记录自动开启下一工序
        logProcessOperation(next.getId(), workerId, "AUTO_START_NEXT", 
                suggestedStartTime != null ? String.format("使用补填工序的结束时间作为开始时间: %s", suggestedStartTime) : null);
        log.info("已自动开启下一工序，当前工序ID: {}, 下一工序ID: {}, 开始时间: {}, 操作员: {}", 
                currentProcess.getId(), next.getId(), startTime, next.getOperatorId());
    }

    /**
     * 更新后续工序的时间（当补填已完成工序的时间时）
     * 如果补填的时间改变了，后续工序的开始时间和结束时间都应该相应调整
     * 
     * @param currentProcess 当前补填的工序
     * @param oldEndTime 补填前的结束时间（可能为null）
     * @param newEndTime 补填后的结束时间
     */
    private void updateSubsequentProcessesTime(Process currentProcess, LocalDateTime oldEndTime, LocalDateTime newEndTime) {
        if (currentProcess == null || currentProcess.getCycleId() == null || currentProcess.getStartOrder() == null || newEndTime == null) {
            return;
        }
        
        log.info("开始更新后续工序时间，当前工序ID: {}, 补填前的结束时间: {}, 补填后的结束时间: {}", 
                currentProcess.getId(), oldEndTime, newEndTime);
        
        // 计算时间差（补填后的结束时间 - 补填前的结束时间）
        long timeDiffMinutes = 0;
        if (oldEndTime != null) {
            timeDiffMinutes = Duration.between(oldEndTime, newEndTime).toMinutes();
        }
        
        // 如果时间没有变化，不需要更新后续工序
        if (oldEndTime != null && timeDiffMinutes == 0) {
            log.debug("补填时间没有变化，不需要更新后续工序，当前工序ID: {}", currentProcess.getId());
            return;
        }
        
        // 获取同一循环下，顺序在当前工序之后的所有工序
        List<Process> subsequentProcesses = lambdaQuery()
                .eq(Process::getCycleId, currentProcess.getCycleId())
                .gt(Process::getStartOrder, currentProcess.getStartOrder())
                .orderByAsc(Process::getStartOrder)
                .list();
        
        if (subsequentProcesses.isEmpty()) {
            log.debug("没有后续工序需要更新，当前工序ID: {}", currentProcess.getId());
            return;
        }
        
        int updatedCount = 0;
        // 使用前一个工序的结束时间作为基准，实现级联更新
        LocalDateTime previousEndTime = newEndTime;
        
        for (Process nextProcess : subsequentProcesses) {
            boolean needUpdate = false;
            LocalDateTime newStartTime = null;
            LocalDateTime newEndTimeForNext = null;
            
            // 判断后续工序的状态，分别处理
            if (ProcessStatus.COMPLETED.getCode().equals(nextProcess.getProcessStatus())) {
                // 已完成工序：调整开始时间和结束时间，保持原耗时
                // 关键逻辑：后续工序的开始时间应该等于前一个工序的结束时间
                LocalDateTime originalStartTime = nextProcess.getActualStartTime();
                if (originalStartTime != null) {
                    // 如果原开始时间不等于前一个工序的新结束时间，需要调整
                    // 这里使用时间差判断，如果时间差超过1分钟，就认为需要调整
                    long timeDiff = Duration.between(originalStartTime, previousEndTime).toMinutes();
                    if (Math.abs(timeDiff) > 1) {
                        // 调整开始时间到前一个工序的新结束时间
                        newStartTime = previousEndTime;
                        needUpdate = true;
                        
                        // 保持原耗时，调整结束时间
                        if (nextProcess.getActualEndTime() != null && originalStartTime != null) {
                            long originalDuration = Duration.between(originalStartTime, nextProcess.getActualEndTime()).toMinutes();
                            newEndTimeForNext = newStartTime.plusMinutes(originalDuration);
                            nextProcess.setActualEndTime(newEndTimeForNext);
                        }
                        nextProcess.setActualStartTime(newStartTime);
                        if (nextProcess.getEstimatedStartTime() != null) {
                            nextProcess.setEstimatedStartTime(newStartTime);
                        }
                        // 更新预计结束时间：新的预计开始时间 + 控制时间
                        if (nextProcess.getControlTime() != null) {
                            LocalDateTime newEstimatedEndTime = newStartTime.plusMinutes(nextProcess.getControlTime());
                            nextProcess.setEstimatedEndTime(newEstimatedEndTime);
                            log.debug("更新已完成工序的预计结束时间，工序ID: {}, 新的预计结束时间: {}", 
                                    nextProcess.getId(), newEstimatedEndTime);
                        }
                        log.debug("更新已完成工序时间，工序ID: {}, 原开始时间: {}, 新开始时间: {}, 新结束时间: {}", 
                                nextProcess.getId(), originalStartTime, newStartTime, newEndTimeForNext);
                    } else {
                        // 如果时间差很小（<=1分钟），认为不需要调整，但需要更新前一个工序的结束时间基准
                        if (nextProcess.getActualEndTime() != null) {
                            newEndTimeForNext = nextProcess.getActualEndTime();
                        }
                    }
                }
            } else if (ProcessStatus.IN_PROGRESS.getCode().equals(nextProcess.getProcessStatus())) {
                // 进行中工序：调整开始时间和预计结束时间，必须清空实际结束时间
                // 关键逻辑：后续工序的开始时间应该等于前一个工序的结束时间
                // 无论是否需要调整开始时间，进行中的工序都不应该有实际结束时间
                
                // 首先清空实际结束时间（进行中的工序不应该有实际结束时间）
                if (nextProcess.getActualEndTime() != null) {
                    nextProcess.setActualEndTime(null);
                    needUpdate = true;
                    log.debug("清空进行中工序的实际结束时间，工序ID: {}", nextProcess.getId());
                }
                
                LocalDateTime originalStartTime = nextProcess.getActualStartTime();
                LocalDateTime originalEstimatedStartTime = nextProcess.getEstimatedStartTime();
                // 确定新的预计开始时间：使用前一个工序的结束时间
                LocalDateTime newEstimatedStartTime = previousEndTime;
                
                // 如果预计开始时间需要更新（与原值不同），则更新
                if (originalEstimatedStartTime == null || !originalEstimatedStartTime.equals(newEstimatedStartTime)) {
                    long timeDiff = 0;
                    if (originalEstimatedStartTime != null) {
                        timeDiff = Duration.between(originalEstimatedStartTime, newEstimatedStartTime).toMinutes();
                    }
                    // 如果时间差超过1分钟，或者预计开始时间为空，需要调整
                    if (originalEstimatedStartTime == null || Math.abs(timeDiff) > 1) {
                        newStartTime = newEstimatedStartTime;
                        needUpdate = true;
                        
                        if (originalStartTime != null) {
                            nextProcess.setActualStartTime(newStartTime);
                        }
                        nextProcess.setEstimatedStartTime(newStartTime);
                        // 重新计算预计结束时间：新的预计开始时间 + 控制时间
                        if (nextProcess.getControlTime() != null) {
                            newEndTimeForNext = newStartTime.plusMinutes(nextProcess.getControlTime());
                            nextProcess.setEstimatedEndTime(newEndTimeForNext);
                        }
                        log.debug("更新进行中工序时间，工序ID: {}, 原预计开始时间: {}, 新预计开始时间: {}, 新预计结束时间: {}", 
                                nextProcess.getId(), originalEstimatedStartTime, newStartTime, newEndTimeForNext);
                    } else {
                        // 如果时间差很小（<=1分钟），认为不需要调整开始时间
                        // 但为了确保预计结束时间正确，仍然重新计算：预计开始时间 + 控制时间
                        if (nextProcess.getControlTime() != null && originalEstimatedStartTime != null) {
                            newEndTimeForNext = originalEstimatedStartTime.plusMinutes(nextProcess.getControlTime());
                            // 只有当计算出的预计结束时间与当前值不同时才更新
                            if (nextProcess.getEstimatedEndTime() == null || 
                                !nextProcess.getEstimatedEndTime().equals(newEndTimeForNext)) {
                                nextProcess.setEstimatedEndTime(newEndTimeForNext);
                                needUpdate = true;
                            }
                        } else if (nextProcess.getEstimatedEndTime() != null) {
                            newEndTimeForNext = nextProcess.getEstimatedEndTime();
                        }
                    }
                } else {
                    // 如果预计开始时间没有改变，确保预计结束时间正确：预计开始时间 + 控制时间
                    if (nextProcess.getControlTime() != null && originalEstimatedStartTime != null) {
                        LocalDateTime expectedEndTime = originalEstimatedStartTime.plusMinutes(nextProcess.getControlTime());
                        if (nextProcess.getEstimatedEndTime() == null || 
                            !nextProcess.getEstimatedEndTime().equals(expectedEndTime)) {
                            nextProcess.setEstimatedEndTime(expectedEndTime);
                            newEndTimeForNext = expectedEndTime;
                            needUpdate = true;
                        } else {
                            newEndTimeForNext = nextProcess.getEstimatedEndTime();
                        }
                    } else if (nextProcess.getEstimatedEndTime() != null) {
                        newEndTimeForNext = nextProcess.getEstimatedEndTime();
                    }
                }
            } else {
                // 未开始工序：设置预计开始时间为前一个工序的结束时间
                // 这样在补填进行中的工序时，下一个工序的预计开始时间会从补填工序的结束时间开始
                LocalDateTime originalEstimatedStartTime = nextProcess.getEstimatedStartTime();
                LocalDateTime newEstimatedStartTime = previousEndTime;
                
                if (originalEstimatedStartTime == null || !originalEstimatedStartTime.equals(newEstimatedStartTime)) {
                    // 如果预计开始时间为空，或者不等于前一个工序的结束时间，需要更新
                    long timeDiff = 0;
                    if (originalEstimatedStartTime != null) {
                        timeDiff = Duration.between(originalEstimatedStartTime, newEstimatedStartTime).toMinutes();
                    }
                    if (originalEstimatedStartTime == null || Math.abs(timeDiff) > 1) {
                        nextProcess.setEstimatedStartTime(newEstimatedStartTime);
                        needUpdate = true;
                        // 重新计算预计结束时间：新的预计开始时间 + 控制时间
                        if (nextProcess.getControlTime() != null) {
                            newEndTimeForNext = newEstimatedStartTime.plusMinutes(nextProcess.getControlTime());
                            nextProcess.setEstimatedEndTime(newEndTimeForNext);
                        }
                        log.debug("更新未开始工序的预计时间，工序ID: {}, 预计开始时间: {}, 预计结束时间: {}", 
                                nextProcess.getId(), newEstimatedStartTime, newEndTimeForNext);
                    } else {
                        // 如果时间差很小（<=1分钟），认为不需要调整开始时间
                        // 但为了确保预计结束时间正确，仍然重新计算：预计开始时间 + 控制时间
                        if (nextProcess.getControlTime() != null && originalEstimatedStartTime != null) {
                            newEndTimeForNext = originalEstimatedStartTime.plusMinutes(nextProcess.getControlTime());
                            // 只有当计算出的预计结束时间与当前值不同时才更新
                            if (nextProcess.getEstimatedEndTime() == null || 
                                !nextProcess.getEstimatedEndTime().equals(newEndTimeForNext)) {
                                nextProcess.setEstimatedEndTime(newEndTimeForNext);
                                needUpdate = true;
                            }
                        } else if (nextProcess.getEstimatedEndTime() != null) {
                            newEndTimeForNext = nextProcess.getEstimatedEndTime();
                        }
                    }
                } else {
                    // 如果预计开始时间已经等于前一个工序的结束时间，确保预计结束时间正确：预计开始时间 + 控制时间
                    if (nextProcess.getControlTime() != null && originalEstimatedStartTime != null) {
                        LocalDateTime expectedEndTime = originalEstimatedStartTime.plusMinutes(nextProcess.getControlTime());
                        if (nextProcess.getEstimatedEndTime() == null || 
                            !nextProcess.getEstimatedEndTime().equals(expectedEndTime)) {
                            nextProcess.setEstimatedEndTime(expectedEndTime);
                            newEndTimeForNext = expectedEndTime;
                            needUpdate = true;
                        } else {
                            newEndTimeForNext = nextProcess.getEstimatedEndTime();
                        }
                    } else if (nextProcess.getEstimatedEndTime() != null) {
                        newEndTimeForNext = nextProcess.getEstimatedEndTime();
                    }
                }
            }
            
            if (needUpdate) {
                updateById(nextProcess);
                logProcessOperation(nextProcess.getId(), null, "UPDATE_TIME_BY_PREVIOUS_FILL", 
                        String.format("因前序工序补填时间而调整：新开始时间=%s", newStartTime));
                updatedCount++;
            }
            
            // 更新前一个工序的结束时间基准，用于下一个工序的判断
            if (newEndTimeForNext != null) {
                previousEndTime = newEndTimeForNext;
            } else if (needUpdate && newStartTime != null && nextProcess.getControlTime() != null) {
                // 如果没有结束时间，但调整了开始时间，使用开始时间+控制时间作为基准
                previousEndTime = newStartTime.plusMinutes(nextProcess.getControlTime());
            } else if (!needUpdate) {
                // 如果没有调整，使用当前工序的结束时间作为基准（如果有的话）
                LocalDateTime currentEndTime = null;
                if (ProcessStatus.COMPLETED.getCode().equals(nextProcess.getProcessStatus())) {
                    currentEndTime = nextProcess.getActualEndTime();
                } else {
                    currentEndTime = nextProcess.getEstimatedEndTime();
                }
                if (currentEndTime != null) {
                    previousEndTime = currentEndTime;
                }
            }
        }
        
        log.info("更新后续工序时间完成，当前工序ID: {}, 后续工序总数: {}, 已更新数量: {}", 
                currentProcess.getId(), subsequentProcesses.size(), updatedCount);
    }
    
    /**
     * 记录工序操作日志
     */
    private void logProcessOperation(Long processId, Long userId, String action, String remark) {
        try {
            ProcessOperationLog log = new ProcessOperationLog();
            log.setProcessId(processId);
            log.setUserId(userId);
            log.setAction(action);
            log.setRemark(remark);
            log.setCreateTime(LocalDateTime.now());
            processOperationLogService.save(log);
        } catch (Exception e) {
            log.warn("记录工序操作日志失败，processId: {}, userId: {}, action: {}, error: {}", processId, userId, action, e.getMessage());
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
        
        // 对于已完成工序，确保预计结束时间 = 预计开始时间 + 控制时间
        if (ProcessStatus.COMPLETED.getCode().equals(process.getProcessStatus()) 
                && process.getEstimatedStartTime() != null 
                && process.getControlTime() != null) {
            LocalDateTime calculatedEstimatedEndTime = process.getEstimatedStartTime().plusMinutes(process.getControlTime());
            response.setEstimatedEndTime(calculatedEstimatedEndTime);
        } else {
            response.setEstimatedEndTime(process.getEstimatedEndTime());
        }

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
        
        // 判断是否需要补填时间
        // 规则：
        // 1. 已完成的工序：不需要补填，直接返回 false
        // 2. 进行中的工序：如果已进行时间 > 控制时间（超时），则需要补填
        // 3. 未开始的工序：不需要补填
        boolean isCompleted = ProcessStatus.COMPLETED.getCode().equals(process.getProcessStatus());
        boolean isInProgress = ProcessStatus.IN_PROGRESS.getCode().equals(process.getProcessStatus());
        Boolean needsTimeFill = false; // 默认为 false
        
        if (isCompleted) {
            // 已完成的工序不需要补填，直接返回 false
            needsTimeFill = false;
        } else if (isInProgress) {
            // 工序进行中：判断是否超时
            if (process.getActualStartTime() != null && process.getControlTime() != null) {
                // 计算从实际开始时间到当前时间已经过了多少分钟
                long elapsedMinutes = Duration.between(process.getActualStartTime(), LocalDateTime.now()).toMinutes();
                // 如果已进行时间 > 控制时间（超时），则需要补填
                if (elapsedMinutes > process.getControlTime()) {
                    needsTimeFill = true;
                }
                // 如果已进行时间 <= 控制时间，则不需要补填（保持 false）
            }
        }
        // 未开始的工序不进行判断，needsTimeFill 保持为 false
        response.setNeedsTimeFill(needsTimeFill);

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
        response.setActualStartTime(process.getActualStartTime());
        response.setActualEndTime(process.getActualEndTime());
        response.setCreateTime(process.getCreateTime());
        response.setUpdateTime(process.getUpdateTime());
        
        // 对于已完成工序，确保预计结束时间 = 预计开始时间 + 控制时间
        if (ProcessStatus.COMPLETED.getCode().equals(process.getProcessStatus()) 
                && process.getEstimatedStartTime() != null 
                && process.getControlTime() != null) {
            LocalDateTime calculatedEstimatedEndTime = process.getEstimatedStartTime().plusMinutes(process.getControlTime());
            response.setEstimatedEndTime(calculatedEstimatedEndTime);
        } else {
            response.setEstimatedEndTime(process.getEstimatedEndTime());
        }

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
        
        // 验证所有工序都属于该循环，且不能是已完成的工序
        List<Process> processesToUpdate = new ArrayList<>();
        for (UpdateProcessOrderRequest.ProcessOrderItem item : request.getProcessOrders()) {
            Process process = getById(item.getProcessId());
            if (process == null) {
                throw new BusinessException("工序不存在，工序ID: " + item.getProcessId());
            }
            if (!process.getCycleId().equals(cycleId)) {
                throw new BusinessException("工序不属于该循环，工序ID: " + item.getProcessId());
            }
            // 检查工序是否已完成，已完成的工序不能修改开始顺序
            if (ProcessStatus.COMPLETED.getCode().equals(process.getProcessStatus())) {
                log.error("批量更新工序顺序失败，已完成的工序不能修改开始顺序，工序ID: {}", item.getProcessId());
                throw new BusinessException(ResultCode.PROCESS_COMPLETED_CANNOT_UPDATE_ORDER);
            }
            processesToUpdate.add(process);
        }
        
        // 获取该循环下的所有工序
        List<Process> allProcesses = getProcessesByCycleId(cycleId);
        
        // 构建工序ID到新顺序的映射
        Map<Long, Integer> newOrderMap = request.getProcessOrders().stream()
                .collect(Collectors.toMap(
                        UpdateProcessOrderRequest.ProcessOrderItem::getProcessId,
                        UpdateProcessOrderRequest.ProcessOrderItem::getStartOrder));
        
        // 构建工序ID到旧顺序的映射（用于交换）
        Map<Long, Integer> oldOrderMap = processesToUpdate.stream()
                .collect(Collectors.toMap(Process::getId, Process::getStartOrder));
        
        // 批量更新工序顺序，处理交换逻辑
        for (UpdateProcessOrderRequest.ProcessOrderItem item : request.getProcessOrders()) {
            Process process = getById(item.getProcessId());
            if (process == null) {
                continue;
            }
            
            Integer newOrder = item.getStartOrder();
            Integer oldOrder = oldOrderMap.get(process.getId());
            
            // 如果新顺序与当前顺序不同
            if (!Objects.equals(newOrder, oldOrder)) {
                // 检查是否有其他工序占用该顺序
                Process targetProcess = allProcesses.stream()
                        .filter(p -> Objects.equals(p.getStartOrder(), newOrder)
                                && !p.getId().equals(process.getId()))
                        .findFirst()
                        .orElse(null);
                
                if (targetProcess != null) {
                    // 检查目标工序是否在本次更新列表中
                    boolean targetInUpdateList = newOrderMap.containsKey(targetProcess.getId());
                    
                    if (targetInUpdateList) {
                        // 目标工序也在更新列表中，检查是否要交换
                        Integer targetNewOrder = newOrderMap.get(targetProcess.getId());
                        if (Objects.equals(targetNewOrder, oldOrder)) {
                            // 两个工序要交换顺序，直接交换
                            process.setStartOrder(newOrder);
                            updateById(process);
                            log.debug("交换工序顺序（批量更新），工序ID: {} ({} -> {}) 和工序ID: {} ({} -> {})", 
                                    process.getId(), oldOrder, newOrder, 
                                    targetProcess.getId(), newOrder, oldOrder);
                        } else {
                            // 目标工序也要更新到其他顺序，检查冲突
                            log.error("批量更新工序顺序失败，目标顺序 {} 已被工序占用，工序ID: {}", 
                                    newOrder, targetProcess.getId());
                            throw new BusinessException("目标顺序 " + newOrder + " 已被其他工序占用，无法修改");
                        }
                    } else {
                        // 目标工序不在更新列表中
                        if (ProcessStatus.NOT_STARTED.getCode().equals(targetProcess.getProcessStatus())) {
                            // 如果目标顺序被另一个未开始工序占用，则交换两个工序的顺序
                            targetProcess.setStartOrder(oldOrder);
                            updateById(targetProcess);
                            process.setStartOrder(newOrder);
                            updateById(process);
                            log.info("交换工序顺序，工序ID: {} ({} -> {}) 和工序ID: {} ({} -> {})", 
                                    process.getId(), oldOrder, newOrder, 
                                    targetProcess.getId(), newOrder, oldOrder);
                        } else {
                            // 如果目标顺序被已完成或进行中的工序占用，抛出异常
                            log.error("批量更新工序顺序失败，目标顺序 {} 已被{}工序占用，工序ID: {}", 
                                    newOrder, targetProcess.getProcessStatus(), targetProcess.getId());
                            throw new BusinessException("目标顺序 " + newOrder + " 已被其他工序占用，无法修改");
                        }
                    }
                } else {
                    // 目标顺序未被占用，直接设置
                    process.setStartOrder(newOrder);
                    updateById(process);
                }
            }
            
            log.debug("更新工序顺序，工序ID: {}, 新顺序: {}", item.getProcessId(), item.getStartOrder());
        }
        
        log.info("批量更新工序顺序完成，循环ID: {}, 更新数量: {}", cycleId, request.getProcessOrders().size());
    }

    @Override
    public CycleProcessTimeResponse calculateCycleProcessTime(Long cycleId) {
        log.info("计算循环工序总时间，循环ID: {}", cycleId);
        Cycle cycle = cycleMapper.selectById(cycleId);
        if (cycle == null) {
            throw new BusinessException(ResultCode.CYCLE_NOT_FOUND);
        }

        List<Process> processes = getProcessesByCycleId(cycleId);
        
        CycleProcessTimeResponse response = new CycleProcessTimeResponse();
        response.setCycleId(cycleId);
        response.setCycleNumber(cycle.getCycleNumber());
        response.setTotalProcessCount(processes.size());

        // 计算单工序总时间（所有工序实际完成时间的总和，不考虑重叠）
        long totalIndividualTime = 0;
        int completedCount = 0;
        List<CycleProcessTimeResponse.ProcessTimeDetail> details = new ArrayList<>();

        // 收集所有已完成工序的时间段
        List<TimeInterval> intervals = new ArrayList<>();
        
        for (Process process : processes) {
            CycleProcessTimeResponse.ProcessTimeDetail detail =
                new CycleProcessTimeResponse.ProcessTimeDetail();
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
        List<TimeInterval> merged = new ArrayList<>();
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
        
        logProcessOperation(processId, workerId, "OVERTIME_REASON", overtimeReason);

        log.info("施工人员填报超时原因成功，工序ID: {}", processId);
    }

    @Override
    public Page<OvertimeProcessResponse> getOvertimeProcessesWithoutReason(
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
                ? Collections.emptyList()
                : cycleMapper.selectBatchIds(cycleIds);
        Map<Long, Cycle> cycleMap = cycles.stream()
                .filter(cycle -> !"COMPLETED".equals(cycle.getStatus())) // 只保留未完成的循环
                .collect(Collectors.toMap(Cycle::getId, c -> c));
        
        // 获取项目信息
        List<Long> projectIds = cycles.stream().map(Cycle::getProjectId).distinct().toList();
        List<Project> projects = projectIds.isEmpty()
                ? Collections.emptyList()
                : projectMapper.selectBatchIds(projectIds);
        Map<Long, Project> projectMap = projects.stream()
                .collect(Collectors.toMap(Project::getId, p -> p));
        
        // 获取操作员信息
        List<Long> operatorIds = processes.stream()
                .map(Process::getOperatorId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        List<User> operators = operatorIds.isEmpty()
                ? Collections.emptyList()
                : userService.listByIds(operatorIds);
        Map<Long, User> operatorMap = operators.stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        
        // 构建响应列表
        List<OvertimeProcessResponse> result = new ArrayList<>();
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
            
            OvertimeProcessResponse response = new OvertimeProcessResponse();
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
        
        Page<OvertimeProcessResponse> resultPage = new Page<>(pageNum, pageSize, result.size());
        resultPage.setRecords(result);
        
        log.info("查询超时未填报原因的工序列表完成，找到 {} 条记录", result.size());
        return resultPage;
    }
    
    /**
     * 调整工序顺序：将指定位置及之后的所有工序的 startOrder +1
     * 用于在插入新工序时避免顺序冲突
     * 
     * @param cycleId 循环ID
     * @param insertOrder 要插入的位置（新工序的 startOrder）
     */
    private void adjustProcessOrdersForInsert(Long cycleId, Integer insertOrder) {
        log.debug("调整工序顺序，循环ID: {}, 插入位置: {}", cycleId, insertOrder);
        
        // 获取所有需要调整的工序（startOrder >= insertOrder）
        List<Process> processesToAdjust = list(new LambdaQueryWrapper<Process>()
                .eq(Process::getCycleId, cycleId)
                .ge(Process::getStartOrder, insertOrder)
                .orderByAsc(Process::getStartOrder));
        
        if (processesToAdjust.isEmpty()) {
            return;
        }
        
        // 批量更新：每个工序的 startOrder +1
        for (Process process : processesToAdjust) {
            process.setStartOrder(process.getStartOrder() + 1);
            updateById(process);
            log.debug("调整工序顺序，工序ID: {}, 工序名称: {}, 新顺序: {}", 
                    process.getId(), process.getProcessName(), process.getStartOrder());
        }
        
        log.info("工序顺序调整完成，循环ID: {}, 调整数量: {}", cycleId, processesToAdjust.size());
    }
}

