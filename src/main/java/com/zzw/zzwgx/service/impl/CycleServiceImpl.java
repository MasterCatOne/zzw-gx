package com.zzw.zzwgx.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzw.zzwgx.common.enums.ProcessStatus;
import com.zzw.zzwgx.common.enums.ResultCode;
import com.zzw.zzwgx.common.exception.BusinessException;
import com.zzw.zzwgx.dto.request.CreateCycleRequest;
import com.zzw.zzwgx.dto.response.CycleResponse;
import com.zzw.zzwgx.entity.Cycle;
import com.zzw.zzwgx.entity.Process;
import com.zzw.zzwgx.entity.ProcessTemplate;
import com.zzw.zzwgx.entity.Project;
import com.zzw.zzwgx.mapper.CycleMapper;
import com.zzw.zzwgx.mapper.ProjectMapper;
import com.zzw.zzwgx.service.CycleService;
import com.zzw.zzwgx.service.ProcessService;
import com.zzw.zzwgx.service.ProcessTemplateService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 循环服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CycleServiceImpl extends ServiceImpl<CycleMapper, Cycle> implements CycleService {
    
    private final ProjectMapper projectMapper;
    private final ProcessTemplateService processTemplateService;
    private final ProcessService processService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CycleResponse createCycle(CreateCycleRequest request) {
        log.info("开始创建循环，项目ID: {}, 模板ID: {}", request.getProjectId(), request.getTemplateId());
        // 验证项目是否存在 - 直接使用Mapper避免循环依赖
        Project project = projectMapper.selectById(request.getProjectId());
        if (project == null) {
            log.error("创建循环失败，项目不存在，项目ID: {}", request.getProjectId());
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        
        // 验证模板是否存在
        ProcessTemplate template = processTemplateService.getById(request.getTemplateId());
        if (template == null) {
            log.error("创建循环失败，模板不存在，模板ID: {}", request.getTemplateId());
            throw new BusinessException(ResultCode.TEMPLATE_NOT_FOUND);
        }
        
        // 获取当前循环次数
        Cycle latestCycle = getLatestCycleByProjectId(request.getProjectId());
        int cycleNumber = latestCycle != null ? latestCycle.getCycleNumber() + 1 : 1;
        log.debug("计算循环次数，项目ID: {}, 当前循环次数: {}", request.getProjectId(), cycleNumber);
        
        // 创建循环
        Cycle cycle = new Cycle();
        cycle.setProjectId(request.getProjectId());
        cycle.setCycleNumber(cycleNumber);
        cycle.setControlDuration(request.getControlDuration());
        cycle.setStartDate(request.getStartDate());
        if (request.getEstimatedMileage() != null) {
            cycle.setEstimatedMileage(BigDecimal.valueOf(request.getEstimatedMileage()));
        }
        cycle.setStatus("IN_PROGRESS");
        cycle.setAdvanceLength(BigDecimal.ZERO);
        cycle.setTemplateId(request.getTemplateId());
        save(cycle);
        log.info("循环创建成功，循环ID: {}, 循环次数: {}", cycle.getId(), cycleNumber);
        
        // 更新项目的当前循环次数 - 直接使用Mapper避免循环依赖
        project.setCurrentCycle(cycleNumber);
        projectMapper.updateById(project);
        log.debug("更新项目当前循环次数，项目ID: {}, 当前循环次数: {}", project.getId(), cycleNumber);
        
        // 根据模板创建工序
        try {
            List<Map<String, Object>> processList = objectMapper.readValue(
                    template.getProcessList(), 
                    new TypeReference<List<Map<String, Object>>>() {}
            );
            log.debug("从模板解析工序列表，模板ID: {}, 工序数量: {}", request.getTemplateId(), processList.size());
            
            for (Map<String, Object> processMap : processList) {
                Process process = new Process();
                process.setCycleId(cycle.getId());
                process.setName((String) processMap.get("name"));
                process.setControlTime(((Number) processMap.get("controlTime")).intValue());
                process.setStatus(ProcessStatus.NOT_STARTED.getCode());
                process.setStartOrder(((Number) processMap.get("startOrder")).intValue());
                if (processMap.get("advanceLength") != null) {
                    process.setAdvanceLength(BigDecimal.valueOf(((Number) processMap.get("advanceLength")).doubleValue()));
                } else {
                    process.setAdvanceLength(BigDecimal.ZERO);
                }
                processService.save(process);
            }
            log.info("根据模板创建工序完成，循环ID: {}, 工序数量: {}", cycle.getId(), processList.size());
        } catch (Exception e) {
            log.error("创建工序失败，模板格式错误，循环ID: {}, 错误信息: {}", cycle.getId(), e.getMessage());
            throw new BusinessException("工序模板格式错误");
        }
        
        return BeanUtil.copyProperties(cycle, CycleResponse.class);
    }
    
    @Override
    public Cycle getCurrentCycleByProjectId(Long projectId) {
        log.debug("查询项目当前循环，项目ID: {}", projectId);
        LambdaQueryWrapper<Cycle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cycle::getProjectId, projectId)
                .eq(Cycle::getStatus, "IN_PROGRESS")
                .orderByDesc(Cycle::getCycleNumber)
                .last("LIMIT 1");
        Cycle cycle = getOne(wrapper);
        if (cycle != null) {
            log.debug("查询到当前循环，项目ID: {}, 循环ID: {}, 循环次数: {}", projectId, cycle.getId(), cycle.getCycleNumber());
        } else {
            log.debug("未查询到当前循环，项目ID: {}", projectId);
        }
        return cycle;
    }
    
    @Override
    public Cycle getLatestCycleByProjectId(Long projectId) {
        log.debug("查询项目最新循环，项目ID: {}", projectId);
        LambdaQueryWrapper<Cycle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cycle::getProjectId, projectId)
                .orderByDesc(Cycle::getCycleNumber)
                .last("LIMIT 1");
        Cycle cycle = getOne(wrapper);
        if (cycle != null) {
            log.debug("查询到最新循环，项目ID: {}, 循环ID: {}, 循环次数: {}", projectId, cycle.getId(), cycle.getCycleNumber());
        }
        return cycle;
    }
}

