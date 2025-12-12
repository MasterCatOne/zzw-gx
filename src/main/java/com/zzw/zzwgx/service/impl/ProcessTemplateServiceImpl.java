package com.zzw.zzwgx.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzw.zzwgx.common.enums.ResultCode;
import com.zzw.zzwgx.common.exception.BusinessException;
import com.zzw.zzwgx.dto.request.UpdateProcessTemplateRequest;
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
import org.springframework.transaction.annotation.Transactional;

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
                .eq(ProcessTemplate::getDeleted, 0)
                .orderByAsc(ProcessTemplate::getDefaultOrder);
        List<ProcessTemplate> templates = list(wrapper);
        log.debug("查询到工序模板数量: {}", templates.size());
        return templates;
    }
    
    @Override
    public List<ProcessTemplate> getTemplatesByNameAndSiteId(String templateName, Long siteId) {
        log.debug("根据工点ID和模板名称查询工序模板，工点ID: {}, 模板名称: {}", siteId, templateName);
        LambdaQueryWrapper<ProcessTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessTemplate::getSiteId, siteId)
                .eq(ProcessTemplate::getTemplateName, templateName)
                .eq(ProcessTemplate::getDeleted, 0)
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
                .eq(ProcessTemplate::getDeleted, 0)
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
    public List<String> getTemplateNamesBySiteId(Long siteId) {
        log.debug("根据工点ID查询模板名称列表，工点ID: {}", siteId);
        LambdaQueryWrapper<ProcessTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(ProcessTemplate::getTemplateName)
                .eq(ProcessTemplate::getSiteId, siteId)
                .eq(ProcessTemplate::getDeleted, 0)
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
    public List<TemplateListResponse> getTemplateListBySiteId(Long siteId) {
        log.debug("根据工点ID查询模板列表，工点ID: {}", siteId);
        // 获取该工点下的所有模板名称（去重）
        List<String> templateNames = getTemplateNamesBySiteId(siteId);
        
        // 为每个模板名称获取第一个工序模板的ID
        List<TemplateListResponse> templateList = templateNames.stream()
                .map(templateName -> {
                    List<ProcessTemplate> templates = getTemplatesByNameAndSiteId(templateName, siteId);
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
    
    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void adjustOtherTemplatesOrder(Long siteId, String templateName, Long currentTemplateId, Integer oldOrder, Integer newOrder) {
        log.info("调整同一工点下同一模板名称下其他工序模板的顺序，工点ID: {}, 模板名称: {}, 当前模板ID: {}, 旧顺序: {}, 新顺序: {}", 
                siteId, templateName, currentTemplateId, oldOrder, newOrder);
        
        // 获取同一工点、同一模板名称下的所有工序模板（排除当前正在修改的模板）
        List<ProcessTemplate> otherTemplates = list(new LambdaQueryWrapper<ProcessTemplate>()
                .eq(ProcessTemplate::getSiteId, siteId)
                .eq(ProcessTemplate::getTemplateName, templateName)
                .ne(ProcessTemplate::getId, currentTemplateId)
                .eq(ProcessTemplate::getDeleted, 0)
                .orderByAsc(ProcessTemplate::getDefaultOrder));
        
        if (otherTemplates.isEmpty()) {
            log.debug("没有其他工序模板需要调整，工点ID: {}, 模板名称: {}", siteId, templateName);
            return;
        }
        
        int adjustCount = 0;
        
        // 如果新顺序大于旧顺序（向后移动）
        // 例如：将顺序1改为2，那么原来顺序在 [oldOrder+1, newOrder] 范围内的模板应该-1（交换位置）
        // 结果：顺序1的变成2，顺序2的变成1
        if (newOrder > oldOrder) {
            // 将顺序在 [oldOrder+1, newOrder] 范围内的其他模板顺序 -1（交换位置）
            for (ProcessTemplate otherTemplate : otherTemplates) {
                Integer otherOrder = otherTemplate.getDefaultOrder();
                if (otherOrder != null && otherOrder > oldOrder && otherOrder <= newOrder) {
                    otherTemplate.setDefaultOrder(otherOrder - 1);
                    updateById(otherTemplate);
                    adjustCount++;
                    log.debug("调整工序模板顺序（向后移动），模板ID: {}, 工序名称: {}, 旧顺序: {}, 新顺序: {}", 
                            otherTemplate.getId(), otherTemplate.getProcessName(), otherOrder, otherOrder - 1);
                }
            }
        } 
        // 如果新顺序小于旧顺序（向前移动）
        // 例如：将顺序2改为1，那么原来顺序在 [newOrder, oldOrder-1] 范围内的模板应该+1（交换位置）
        // 结果：顺序2的变成1，顺序1的变成2
        else if (newOrder < oldOrder) {
            // 将顺序在 [newOrder, oldOrder-1] 范围内的其他模板顺序 +1（交换位置）
            for (ProcessTemplate otherTemplate : otherTemplates) {
                Integer otherOrder = otherTemplate.getDefaultOrder();
                if (otherOrder != null && otherOrder >= newOrder && otherOrder < oldOrder) {
                    otherTemplate.setDefaultOrder(otherOrder + 1);
                    updateById(otherTemplate);
                    adjustCount++;
                    log.debug("调整工序模板顺序（向前移动），模板ID: {}, 工序名称: {}, 旧顺序: {}, 新顺序: {}", 
                            otherTemplate.getId(), otherTemplate.getProcessName(), otherOrder, otherOrder + 1);
                }
            }
        }
        
        log.info("调整同一工点下同一模板名称下其他工序模板的顺序完成，工点ID: {}, 模板名称: {}, 调整数量: {}", siteId, templateName, adjustCount);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessTemplateResponse updateProcessTemplate(Long templateId, UpdateProcessTemplateRequest request) {
        log.info("更新工序模板，模板ID: {}", templateId);
        ProcessTemplate template = getById(templateId);
        if (template == null) {
            throw new BusinessException(ResultCode.TEMPLATE_NOT_FOUND);
        }
        
        // 记录旧的默认顺序，用于调整其他模板顺序
        Integer oldDefaultOrder = template.getDefaultOrder();
        String templateName = template.getTemplateName();
        Long siteId = template.getSiteId();
        
        // 如果默认顺序发生变化，需要先调整同一工点下同一模板名称下其他工序模板的顺序（避免冲突）
        // 注意：必须先调整其他模板，再更新当前模板，避免顺序冲突
        if (request.getDefaultOrder() != null && oldDefaultOrder != null 
                && !oldDefaultOrder.equals(request.getDefaultOrder())) {
            log.info("工序模板默认顺序发生变化，模板ID: {}, 工点ID: {}, 模板名称: {}, 旧顺序: {}, 新顺序: {}", 
                    templateId, siteId, templateName, oldDefaultOrder, request.getDefaultOrder());
            
            // 先调整同一工点下同一模板名称下其他工序模板的顺序（为当前模板让出位置）
            adjustOtherTemplatesOrder(siteId, templateName, templateId, oldDefaultOrder, request.getDefaultOrder());
        }
        
        // 更新当前模板的字段
        if (request.getTemplateName() != null) {
            template.setTemplateName(request.getTemplateName());
        }
        if (request.getProcessCatalogId() != null) {
            ProcessCatalog catalog = processCatalogService.getById(request.getProcessCatalogId());
            if (catalog == null) {
                throw new BusinessException("工序字典不存在，ID: " + request.getProcessCatalogId());
            }
            template.setProcessCatalogId(request.getProcessCatalogId());
            template.setProcessName(catalog.getProcessName()); // 冗余名称
        } else if (request.getProcessName() != null) {
            // 仅向后兼容，优先使用字典
            template.setProcessName(request.getProcessName());
        }
        if (request.getControlTime() != null) {
            template.setControlTime(request.getControlTime());
        }
        if (request.getDefaultOrder() != null) {
            template.setDefaultOrder(request.getDefaultOrder());
        }
        if (request.getDescription() != null) {
            template.setDescription(request.getDescription());
        }
        // 最后更新当前模板（包括新的默认顺序）
        updateById(template);
        
        return convertToResponse(template);
    }
    
    @Override
    public List<com.zzw.zzwgx.dto.response.TemplateWithProcessesResponse> getTemplatesWithProcesses() {
        log.info("查询所有模板及其工序列表");
        
        // 获取所有模板名称（去重）
        List<String> templateNames = getAllTemplateNames();
        
        // 为每个模板名称获取其下的所有工序
        List<com.zzw.zzwgx.dto.response.TemplateWithProcessesResponse> result = templateNames.stream()
                .map(templateName -> {
                    // 获取该模板下的所有工序模板（按默认顺序排序）
                    List<ProcessTemplate> templates = getTemplatesByName(templateName);
                    if (templates.isEmpty()) {
                        return null;
                    }
                    
                    // 转换为响应对象
                    com.zzw.zzwgx.dto.response.TemplateWithProcessesResponse response = 
                            new com.zzw.zzwgx.dto.response.TemplateWithProcessesResponse();
                    response.setTemplateName(templateName);
                    
                    // 获取该模板下第一个工序模板的ID（作为模板ID）
                    ProcessTemplate firstTemplate = templates.get(0);
                    response.setTemplateId(firstTemplate.getId());
                    
                    // 转换所有工序模板为响应对象
                    List<com.zzw.zzwgx.dto.response.ProcessTemplateResponse> processResponses = templates.stream()
                            .map(this::convertToResponse)
                            .collect(Collectors.toList());
                    response.setProcesses(processResponses);
                    
                    return response;
                })
                .filter(template -> template != null)
                .collect(Collectors.toList());
        
        log.info("查询到模板数量: {}", result.size());
        return result;
    }
}

