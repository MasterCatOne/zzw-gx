package com.zzw.zzwgx.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzw.zzwgx.dto.response.ProcessTemplateOptionResponse;
import com.zzw.zzwgx.dto.response.ProcessTemplateResponse;
import com.zzw.zzwgx.dto.response.TemplateListResponse;
import com.zzw.zzwgx.entity.ProcessCatalog;
import com.zzw.zzwgx.entity.ProcessTemplate;
import com.zzw.zzwgx.mapper.ProcessTemplateMapper;
import com.zzw.zzwgx.service.ProcessCatalogService;
import com.zzw.zzwgx.service.ProcessTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 工序模板服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessTemplateServiceImpl extends ServiceImpl<ProcessTemplateMapper, ProcessTemplate> implements ProcessTemplateService {
    
    private final ProcessCatalogService processCatalogService;
    
    @Override
    public List<ProcessTemplate> getTemplatesByName(String templateName) {
        log.debug("根据模板名称查询工序模板，模板名称: {}", templateName);
        LambdaQueryWrapper<ProcessTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessTemplate::getTemplateName, templateName)
                .orderByAsc(ProcessTemplate::getDefaultOrder);
        List<ProcessTemplate> templates = list(wrapper);
        log.debug("查询到工序模板数量: {}", templates.size());
        return templates;
    }
    
    @Override
    public List<ProcessTemplate> getTemplatesByTemplateId(Long templateId) {
        log.debug("根据模板ID查询工序模板，模板ID: {}", templateId);
        // 这里假设templateId对应的是模板名称，实际可以根据业务需求调整
        // 如果templateId是模板的唯一标识，可以添加template_id字段
        // 当前实现：根据模板名称查询，templateId作为名称使用
        ProcessTemplate template = getById(templateId);
        if (template == null) {
            log.warn("模板不存在，模板ID: {}", templateId);
            return List.of();
        }
        return getTemplatesByName(template.getTemplateName());
    }
    
    @Override
    public List<String> getAllTemplateNames() {
        log.debug("查询所有模板名称列表");
        LambdaQueryWrapper<ProcessTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(ProcessTemplate::getTemplateName)
                .groupBy(ProcessTemplate::getTemplateName);
        List<ProcessTemplate> templates = list(wrapper);
        List<String> templateNames = templates.stream()
                .map(ProcessTemplate::getTemplateName)
                .distinct()
                .collect(Collectors.toList());
        log.debug("查询到模板名称数量: {}", templateNames.size());
        return templateNames;
    }
    
    @Override
    public List<ProcessTemplateOptionResponse> getAllProcessTemplateOptions() {
        log.debug("查询所有工序选项列表（从工序字典表获取，按显示顺序排序）");
        
        // 从工序字典表获取所有工序（已去重，按显示顺序排序）
        List<ProcessCatalog> catalogs = processCatalogService.list(new LambdaQueryWrapper<ProcessCatalog>()
                .eq(ProcessCatalog::getDeleted, 0)
                .eq(ProcessCatalog::getStatus, 1)
                .orderByAsc(ProcessCatalog::getDisplayOrder)
                .orderByAsc(ProcessCatalog::getId));
        
        // 转换为响应DTO
        // 为了兼容前端，需要返回templateId，这里返回该工序在第一个模板中的templateId（如果存在）
        List<ProcessTemplateOptionResponse> options = catalogs.stream()
                .map(catalog -> {
                    ProcessTemplateOptionResponse option = new ProcessTemplateOptionResponse();
                    
                    // 查找该工序在第一个模板中的templateId（用于兼容前端）
                    ProcessTemplate firstTemplate = getOne(new LambdaQueryWrapper<ProcessTemplate>()
                            .eq(ProcessTemplate::getProcessCatalogId, catalog.getId())
                            .eq(ProcessTemplate::getDeleted, 0)
                            .orderByAsc(ProcessTemplate::getTemplateName)
                            .orderByAsc(ProcessTemplate::getDefaultOrder)
                            .last("LIMIT 1"));
                    
                    if (firstTemplate != null) {
                        option.setTemplateId(firstTemplate.getId());
                        option.setTemplateName(firstTemplate.getTemplateName());
                    } else {
                        // 如果该工序不在任何模板中，设置templateId为null或0
                        option.setTemplateId(null);
                        option.setTemplateName(null);
                    }
                    
                    option.setProcessName(catalog.getProcessName());
                    return option;
                })
                .collect(Collectors.toList());
        
        log.debug("查询到工序选项数量: {}", options.size());
        return options;
    }

    @Override
    public List<TemplateListResponse> getTemplateList() {
        log.debug("查询模板列表");
        // 获取所有模板名称（去重）
        List<String> templateNames = getAllTemplateNames();
        
        // 为每个模板名称获取第一个工序模板的ID
        List<TemplateListResponse> templateList = templateNames.stream()
                .map(templateName -> {
                    List<ProcessTemplate> templates = getTemplatesByName(templateName);
                    if (templates.isEmpty()) {
                        return null;
                    }
                    // 获取该模板下第一个工序模板（按defaultOrder排序后的第一个）
                    ProcessTemplate firstTemplate = templates.get(0);
                    TemplateListResponse response = new TemplateListResponse();
                    response.setTemplateName(templateName);
                    response.setTemplateId(firstTemplate.getId());
                    return response;
                })
                .filter(template -> template != null)
                .collect(Collectors.toList());
        
        log.debug("查询到模板数量: {}", templateList.size());
        return templateList;
    }

    @Override
    public ProcessTemplateResponse convertToResponse(ProcessTemplate template) {
        if (template == null) {
            return null;
        }
        ProcessTemplateResponse resp = BeanUtil.copyProperties(template, ProcessTemplateResponse.class);
        // 如果工序名称为空且存在字典ID，从字典补充名称
        if (resp.getProcessCatalogId() != null && (resp.getProcessName() == null || resp.getProcessName().isBlank())) {
            ProcessCatalog catalog = processCatalogService.getById(resp.getProcessCatalogId());
            if (catalog != null) {
                resp.setProcessName(catalog.getProcessName());
            }
        }
        return resp;
    }
}

