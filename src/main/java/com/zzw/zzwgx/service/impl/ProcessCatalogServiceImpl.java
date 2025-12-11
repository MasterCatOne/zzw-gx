package com.zzw.zzwgx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzw.zzwgx.common.enums.ResultCode;
import com.zzw.zzwgx.common.exception.BusinessException;
import com.zzw.zzwgx.dto.request.CreateProcessCatalogRequest;
import com.zzw.zzwgx.dto.request.UpdateProcessCatalogOrderRequest;
import com.zzw.zzwgx.dto.request.UpdateProcessCatalogRequest;
import com.zzw.zzwgx.dto.response.ProcessCatalogResponse;
import com.zzw.zzwgx.entity.ProcessCatalog;
import com.zzw.zzwgx.mapper.ProcessCatalogMapper;
import com.zzw.zzwgx.service.ProcessCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 工序字典服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessCatalogServiceImpl extends ServiceImpl<ProcessCatalogMapper, ProcessCatalog> implements ProcessCatalogService {
    
    @Override
    public List<ProcessCatalogResponse> getAllProcessCatalogs() {
        log.debug("查询所有工序字典列表（按显示顺序排序）");
        List<ProcessCatalog> catalogs = list(new LambdaQueryWrapper<ProcessCatalog>()
                .eq(ProcessCatalog::getDeleted, 0)
                .orderByAsc(ProcessCatalog::getDisplayOrder)
                .orderByAsc(ProcessCatalog::getId));
        
        return catalogs.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessCatalogResponse createProcessCatalog(CreateProcessCatalogRequest request) {
        log.info("创建工序字典，工序名称: {}", request.getProcessName());
        
        // 检查工序名称是否已存在
        ProcessCatalog existing = getOne(new LambdaQueryWrapper<ProcessCatalog>()
                .eq(ProcessCatalog::getProcessName, request.getProcessName())
                .eq(ProcessCatalog::getDeleted, 0));
        if (existing != null) {
            log.warn("创建工序字典失败，工序名称已存在: {}", request.getProcessName());
            throw new BusinessException(ResultCode.PROCESS_NAME_ALREADY_EXISTS);
        }
        
        // 检查工序编码是否已存在（如果提供了）
        if (StringUtils.hasText(request.getProcessCode())) {
            ProcessCatalog existingByCode = getOne(new LambdaQueryWrapper<ProcessCatalog>()
                    .eq(ProcessCatalog::getProcessCode, request.getProcessCode())
                    .eq(ProcessCatalog::getDeleted, 0));
            if (existingByCode != null) {
                log.warn("创建工序字典失败，工序编码已存在: {}", request.getProcessCode());
                throw new BusinessException(ResultCode.PROCESS_CODE_ALREADY_EXISTS);
            }
        }
        
        // 如果没有指定显示顺序，设置为最大值+1
        Integer displayOrder = request.getDisplayOrder();
        if (displayOrder == null) {
            ProcessCatalog lastCatalog = getOne(new LambdaQueryWrapper<ProcessCatalog>()
                    .eq(ProcessCatalog::getDeleted, 0)
                    .orderByDesc(ProcessCatalog::getDisplayOrder)
                    .last("LIMIT 1"));
            displayOrder = (lastCatalog != null && lastCatalog.getDisplayOrder() != null) 
                    ? lastCatalog.getDisplayOrder() + 1 : 1;
        }
        
        ProcessCatalog catalog = new ProcessCatalog();
        catalog.setProcessName(request.getProcessName());
        catalog.setProcessCode(request.getProcessCode());
        catalog.setDescription(request.getDescription());
        catalog.setCategory(request.getCategory());
        catalog.setDisplayOrder(displayOrder);
        catalog.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        
        save(catalog);
        log.info("创建工序字典成功，工序字典ID: {}, 工序名称: {}", catalog.getId(), catalog.getProcessName());
        
        return convertToResponse(catalog);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessCatalogResponse updateProcessCatalog(Long catalogId, UpdateProcessCatalogRequest request) {
        log.info("更新工序字典，工序字典ID: {}", catalogId);
        
        ProcessCatalog catalog = getById(catalogId);
        if (catalog == null) {
            log.warn("更新工序字典失败，工序字典不存在，ID: {}", catalogId);
            throw new BusinessException(ResultCode.PROCESS_CATALOG_NOT_FOUND);
        }
        
        // 如果修改工序名称，检查是否重复
        if (StringUtils.hasText(request.getProcessName()) && !request.getProcessName().equals(catalog.getProcessName())) {
            ProcessCatalog existing = getOne(new LambdaQueryWrapper<ProcessCatalog>()
                    .eq(ProcessCatalog::getProcessName, request.getProcessName())
                    .eq(ProcessCatalog::getDeleted, 0)
                    .ne(ProcessCatalog::getId, catalogId));
            if (existing != null) {
                log.warn("更新工序字典失败，工序名称已存在: {}", request.getProcessName());
                throw new BusinessException(ResultCode.PROCESS_NAME_ALREADY_EXISTS);
            }
            catalog.setProcessName(request.getProcessName());
        }
        
        // 如果修改工序编码，检查是否重复
        if (StringUtils.hasText(request.getProcessCode()) && !request.getProcessCode().equals(catalog.getProcessCode())) {
            ProcessCatalog existingByCode = getOne(new LambdaQueryWrapper<ProcessCatalog>()
                    .eq(ProcessCatalog::getProcessCode, request.getProcessCode())
                    .eq(ProcessCatalog::getDeleted, 0)
                    .ne(ProcessCatalog::getId, catalogId));
            if (existingByCode != null) {
                log.warn("更新工序字典失败，工序编码已存在: {}", request.getProcessCode());
                throw new BusinessException(ResultCode.PROCESS_CODE_ALREADY_EXISTS);
            }
            catalog.setProcessCode(request.getProcessCode());
        }
        
        if (StringUtils.hasText(request.getDescription())) {
            catalog.setDescription(request.getDescription());
        }
        if (StringUtils.hasText(request.getCategory())) {
            catalog.setCategory(request.getCategory());
        }
        if (request.getDisplayOrder() != null) {
            catalog.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getStatus() != null) {
            catalog.setStatus(request.getStatus());
        }
        
        updateById(catalog);
        log.info("更新工序字典成功，工序字典ID: {}", catalogId);
        
        return convertToResponse(catalog);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProcessCatalogOrder(UpdateProcessCatalogOrderRequest request) {
        log.info("批量更新工序顺序，工序数量: {}", request.getOrders().size());
        
        for (UpdateProcessCatalogOrderRequest.ProcessOrderItem item : request.getOrders()) {
            ProcessCatalog catalog = getById(item.getCatalogId());
            if (catalog == null) {
                log.warn("更新工序顺序失败，工序字典不存在，ID: {}", item.getCatalogId());
                throw new BusinessException(ResultCode.PROCESS_CATALOG_NOT_FOUND);
            }
            catalog.setDisplayOrder(item.getDisplayOrder());
            updateById(catalog);
        }
        
        log.info("批量更新工序顺序成功");
    }
    
    /**
     * 转换为响应DTO
     */
    private ProcessCatalogResponse convertToResponse(ProcessCatalog catalog) {
        ProcessCatalogResponse response = new ProcessCatalogResponse();
        response.setId(catalog.getId());
        response.setProcessName(catalog.getProcessName());
        response.setCategory(catalog.getCategory());
//        response.setProcessCode(catalog.getProcessCode());
//        response.setDescription(catalog.getDescription());
//        response.setDisplayOrder(catalog.getDisplayOrder());
//        response.setStatus(catalog.getStatus());
//        response.setCreateTime(catalog.getCreateTime());
//        response.setUpdateTime(catalog.getUpdateTime());
        return response;
    }
}

