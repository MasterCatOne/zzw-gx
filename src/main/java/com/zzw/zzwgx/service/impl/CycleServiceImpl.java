package com.zzw.zzwgx.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzw.zzwgx.common.enums.ResultCode;
import com.zzw.zzwgx.common.exception.BusinessException;
import com.zzw.zzwgx.dto.request.CreateCycleRequest;
import com.zzw.zzwgx.dto.request.UpdateCycleRequest;
import com.zzw.zzwgx.dto.response.CycleResponse;
import com.zzw.zzwgx.entity.Cycle;
import com.zzw.zzwgx.entity.Project;
import com.zzw.zzwgx.mapper.CycleMapper;
import com.zzw.zzwgx.mapper.ProjectMapper;
import com.zzw.zzwgx.service.CycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.stream.Collectors;

/**
 * 循环服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CycleServiceImpl extends ServiceImpl<CycleMapper, Cycle> implements CycleService {
    
    private final ProjectMapper projectMapper;
    
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
        cycle.setEndDate(request.getEndDate());
        cycle.setEstimatedStartDate(request.getEstimatedStartDate());
        cycle.setEstimatedEndDate(request.getEstimatedEndDate());
        if (request.getEstimatedMileage() != null) {
            cycle.setEstimatedMileage(BigDecimal.valueOf(request.getEstimatedMileage()));
        }
        cycle.setAdvanceLength(request.getAdvanceLength() != null
                ? BigDecimal.valueOf(request.getAdvanceLength())
                : BigDecimal.ZERO);
        cycle.setRockLevel(request.getRockLevel());
        cycle.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "IN_PROGRESS");
        save(cycle);
        log.info("循环创建成功，循环ID: {}, 循环次数: {}", cycle.getId(), cycleNumber);
        return convertToResponse(cycle);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CycleResponse updateCycle(Long cycleId, UpdateCycleRequest request) {
        log.info("更新循环信息，循环ID: {}", cycleId);
        Cycle cycle = getById(cycleId);
        if (cycle == null) {
            throw new BusinessException(ResultCode.CYCLE_NOT_FOUND);
        }
        if (request.getControlDuration() != null) {
            cycle.setControlDuration(request.getControlDuration());
        }
        if (request.getStartDate() != null) {
            cycle.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            cycle.setEndDate(request.getEndDate());
        }
        if (request.getEstimatedStartDate() != null) {
            cycle.setEstimatedStartDate(request.getEstimatedStartDate());
        }
        if (request.getEstimatedEndDate() != null) {
            cycle.setEstimatedEndDate(request.getEstimatedEndDate());
        }
        if (request.getEstimatedMileage() != null) {
            cycle.setEstimatedMileage(request.getEstimatedMileage());
        }
        if (request.getAdvanceLength() != null) {
            cycle.setAdvanceLength(request.getAdvanceLength());
        }
        if (StringUtils.hasText(request.getStatus())) {
            cycle.setStatus(request.getStatus());
        }
        if (StringUtils.hasText(request.getRockLevel())) {
            cycle.setRockLevel(request.getRockLevel());
        }
        updateById(cycle);
        log.info("循环更新完成，循环ID: {}", cycleId);
        return convertToResponse(cycle);
    }
    
    @Override
    public CycleResponse getCycleDetail(Long cycleId) {
        Cycle cycle = getById(cycleId);
        if (cycle == null) {
            throw new BusinessException(ResultCode.CYCLE_NOT_FOUND);
        }
        return convertToResponse(cycle);
    }
    
    @Override
    public Page<CycleResponse> getCyclesByProject(Long projectId, Integer pageNum, Integer pageSize) {
        log.info("分页查询循环列表，项目ID: {}, 页码: {}, 大小: {}", projectId, pageNum, pageSize);
        Page<Cycle> page = page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Cycle>()
                        .eq(Cycle::getProjectId, projectId)
                        .orderByDesc(Cycle::getCycleNumber));
        Page<CycleResponse> responsePage = new Page<>(pageNum, pageSize, page.getTotal());
        responsePage.setRecords(page.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList()));
        return responsePage;
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

    @Override
    public Cycle getCycleByProjectAndNumber(Long projectId, Integer cycleNumber) {
        log.debug("根据项目和循环号查询循环，项目ID: {}, 循环号: {}", projectId, cycleNumber);
        if (cycleNumber == null) {
            return null;
        }
        LambdaQueryWrapper<Cycle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cycle::getProjectId, projectId)
                .eq(Cycle::getCycleNumber, cycleNumber)
                .last("LIMIT 1");
        return getOne(wrapper);
    }
    
    private CycleResponse convertToResponse(Cycle cycle) {
        if (cycle == null) {
            return null;
        }
        return BeanUtil.copyProperties(cycle, CycleResponse.class);
    }
}

