package com.zzw.zzwgx.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzw.zzwgx.common.enums.ProcessStatus;
import com.zzw.zzwgx.common.enums.ResultCode;
import com.zzw.zzwgx.common.exception.BusinessException;
import com.zzw.zzwgx.dto.request.CreateProcessRequest;
import com.zzw.zzwgx.dto.response.ProcessDetailResponse;
import com.zzw.zzwgx.dto.response.ProcessResponse;
import com.zzw.zzwgx.entity.Cycle;
import com.zzw.zzwgx.entity.Process;
import com.zzw.zzwgx.entity.User;
import com.zzw.zzwgx.mapper.CycleMapper;
import com.zzw.zzwgx.mapper.ProcessMapper;
import com.zzw.zzwgx.service.ProcessService;
import com.zzw.zzwgx.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

/**
 * 工序服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessServiceImpl extends ServiceImpl<ProcessMapper, Process> implements ProcessService {
    
    private final CycleMapper cycleMapper;
    private final UserService userService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessResponse createProcess(CreateProcessRequest request) {
        log.info("开始创建工序，循环ID: {}, 工序名称: {}, 施工人员ID: {}", request.getCycleId(), request.getName(), request.getWorkerId());
        
        // 验证循环是否存在 - 直接使用Mapper避免循环依赖
        Cycle cycle = cycleMapper.selectById(request.getCycleId());
        if (cycle == null) {
            log.error("创建工序失败，循环不存在，循环ID: {}", request.getCycleId());
            throw new BusinessException(ResultCode.CYCLE_NOT_FOUND);
        }
        
        Process process = new Process();
        process.setCycleId(request.getCycleId());
        process.setProcessName(request.getName());
        process.setControlTime(request.getControlTime());
        process.setActualStartTime(request.getActualStartTime());
        process.setProcessStatus(ProcessStatus.NOT_STARTED.getCode());
        process.setOperatorId(request.getWorkerId());
        process.setStartOrder(request.getStartOrder());
        process.setAdvanceLength(java.math.BigDecimal.ZERO);
        
        save(process);
        log.info("工序创建成功，工序ID: {}, 工序名称: {}", process.getId(), process.getProcessName());
        return BeanUtil.copyProperties(process, ProcessResponse.class);
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
        
        ProcessDetailResponse response = new ProcessDetailResponse();
        response.setId(process.getId());
        response.setName(process.getProcessName());
        response.setStatus(process.getProcessStatus());
        ProcessStatus status = ProcessStatus.fromCode(process.getProcessStatus());
        response.setStatusDesc(status != null ? status.getDesc() : "");
        response.setControlTime(process.getControlTime());
        response.setActualStartTime(process.getActualStartTime());
        response.setActualEndTime(process.getActualEndTime());
        
        if (process.getOperatorId() != null) {
            User operator = userService.getById(process.getOperatorId());
            if (operator != null) {
                response.setOperatorName(operator.getRealName());
            }
        }
        
        // 计算最终时间
        if (process.getActualStartTime() != null && process.getActualEndTime() != null) {
            long minutes = Duration.between(process.getActualStartTime(), process.getActualEndTime()).toMinutes();
            response.setFinalTime((int) minutes);
            
            // 计算节省时间或超时时间
            if (process.getControlTime() != null) {
                if (minutes < process.getControlTime()) {
                    response.setSavedTime(process.getControlTime() - (int) minutes);
                } else {
                    response.setOvertime((int) minutes - process.getControlTime());
                }
            }
        }
        
        log.info("查询工序详情成功，工序ID: {}, 工序名称: {}", processId, process.getProcessName());
        return response;
    }

}

