package com.zzw.zzwgx.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzw.zzwgx.common.enums.ResultCode;
import com.zzw.zzwgx.common.exception.BusinessException;
import com.zzw.zzwgx.dto.request.CreateProcessTemplateBatchRequest;
import com.zzw.zzwgx.dto.request.CreateProcessTemplateRequest;
import com.zzw.zzwgx.dto.request.UpdateProcessTemplateRequest;
import com.zzw.zzwgx.dto.response.ProcessTemplateOptionResponse;
import com.zzw.zzwgx.dto.response.ProcessTemplateResponse;
import com.zzw.zzwgx.dto.response.TemplateListResponse;
import com.zzw.zzwgx.entity.ProcessCatalog;
import com.zzw.zzwgx.entity.ProcessTemplate;
import com.zzw.zzwgx.entity.Project;
import com.zzw.zzwgx.entity.Template;
import com.zzw.zzwgx.entity.TemplateProcess;
import com.zzw.zzwgx.entity.ProjectTemplate;
import com.zzw.zzwgx.mapper.ProcessTemplateMapper;
import com.zzw.zzwgx.mapper.ProjectMapper;
import com.zzw.zzwgx.mapper.TemplateMapper;
import com.zzw.zzwgx.mapper.TemplateProcessMapper;
import com.zzw.zzwgx.mapper.ProjectTemplateMapper;
import com.zzw.zzwgx.service.ProcessCatalogService;
import com.zzw.zzwgx.service.ProcessTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 工序模板服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessTemplateServiceImpl extends ServiceImpl<ProcessTemplateMapper, ProcessTemplate> implements ProcessTemplateService {
    
    private final ProcessCatalogService processCatalogService;
    private final ProjectMapper projectMapper;
    private final TemplateMapper templateMapper;
    private final TemplateProcessMapper templateProcessMapper;
    private final ProjectTemplateMapper projectTemplateMapper;
    
    @Override
    public List<ProcessTemplate> getTemplatesByName(String templateName) {
        log.debug("根据模板名称查询工序模板，模板名称: {}", templateName);
        // 1. 根据模板名称查询模板ID
        Template template = templateMapper.selectOne(new LambdaQueryWrapper<Template>()
                .eq(Template::getTemplateName, templateName)
                .eq(Template::getDeleted, 0)
                .last("LIMIT 1"));
        if (template == null) {
            log.debug("模板不存在，模板名称: {}", templateName);
            return List.of();
        }
        
        // 2. 根据模板ID查询所有工序
        List<TemplateProcess> templateProcesses = templateProcessMapper.selectList(new LambdaQueryWrapper<TemplateProcess>()
                .eq(TemplateProcess::getTemplateId, template.getId())
                .eq(TemplateProcess::getDeleted, 0)
                .orderByAsc(TemplateProcess::getDefaultOrder));
        
        // 3. 转换为 ProcessTemplate 对象
        List<ProcessTemplate> templates = templateProcesses.stream()
                .map(tp -> {
                    ProcessTemplate pt = BeanUtil.copyProperties(tp, ProcessTemplate.class);
                    pt.setTemplateName(templateName);
                    return pt;
                })
                .collect(Collectors.toList());
        
        log.debug("查询到工序模板数量: {}", templates.size());
        return templates;
    }
    
    @Override
    public List<ProcessTemplate> getTemplatesByNameAndSiteId(String templateName, Long siteId) {
        log.debug("根据工点ID和模板名称查询工序模板，工点ID: {}, 模板名称: {}", siteId, templateName);
        // 1. 根据模板名称查询模板ID
        Template template = templateMapper.selectOne(new LambdaQueryWrapper<Template>()
                .eq(Template::getTemplateName, templateName)
                .eq(Template::getDeleted, 0)
                .last("LIMIT 1"));
        if (template == null) {
            log.debug("模板不存在，模板名称: {}", templateName);
            return List.of();
        }
        
        // 2. 验证该工点是否关联了该模板
        ProjectTemplate projectTemplate = projectTemplateMapper.selectOne(new LambdaQueryWrapper<ProjectTemplate>()
                .eq(ProjectTemplate::getProjectId, siteId)
                .eq(ProjectTemplate::getTemplateId, template.getId())
                .eq(ProjectTemplate::getDeleted, 0)
                .last("LIMIT 1"));
        if (projectTemplate == null) {
            log.debug("该工点未关联该模板，工点ID: {}, 模板ID: {}", siteId, template.getId());
            return List.of();
        }
        
        // 3. 根据模板ID查询所有工序
        List<TemplateProcess> templateProcesses = templateProcessMapper.selectList(new LambdaQueryWrapper<TemplateProcess>()
                .eq(TemplateProcess::getTemplateId, template.getId())
                .eq(TemplateProcess::getDeleted, 0)
                .orderByAsc(TemplateProcess::getDefaultOrder));
        
        // 4. 转换为 ProcessTemplate 对象
        List<ProcessTemplate> templates = templateProcesses.stream()
                .map(tp -> {
                    ProcessTemplate pt = BeanUtil.copyProperties(tp, ProcessTemplate.class);
                    pt.setTemplateName(templateName);
                    pt.setSiteId(siteId);
                    return pt;
                })
                .collect(Collectors.toList());
        
        log.debug("查询到工序模板数量: {}", templates.size());
        return templates;
    }
    
    @Override
    public List<ProcessTemplate> getTemplatesByTemplateId(Long templateId) {
        log.debug("根据模板ID查询工序模板，模板ID: {}", templateId);
        // templateId 现在是 template_process 表的 id，需要先查询 template_process 获取 template_id
        TemplateProcess templateProcess = templateProcessMapper.selectById(templateId);
        if (templateProcess == null) {
            log.warn("模板工序不存在，模板ID: {}", templateId);
            return List.of();
        }
        
        // 根据 template_id 查询模板信息
        Template template = templateMapper.selectById(templateProcess.getTemplateId());
        if (template == null) {
            log.warn("模板不存在，模板ID: {}", templateProcess.getTemplateId());
            return List.of();
        }
        
        // 查询该模板下的所有工序
        return getTemplatesByName(template.getTemplateName());
    }
    
    @Override
    public List<String> getAllTemplateNames() {
        log.debug("查询所有模板名称列表");
        List<Template> templates = templateMapper.selectList(new LambdaQueryWrapper<Template>()
                .eq(Template::getDeleted, 0));
        List<String> templateNames = templates.stream()
                .map(Template::getTemplateName)
                .distinct()
                .collect(Collectors.toList());
        log.debug("查询到模板名称数量: {}", templateNames.size());
        return templateNames;
    }
    
    @Override
    public List<String> getTemplateNamesBySiteId(Long siteId) {
        log.debug("根据工点ID查询模板名称列表，工点ID: {}", siteId);
        // 1. 查询该工点关联的所有模板ID
        List<ProjectTemplate> projectTemplates = projectTemplateMapper.selectList(new LambdaQueryWrapper<ProjectTemplate>()
                .eq(ProjectTemplate::getProjectId, siteId)
                .eq(ProjectTemplate::getDeleted, 0));
        
        if (projectTemplates.isEmpty()) {
            return List.of();
        }
        
        // 2. 根据模板ID查询模板名称
        List<Long> templateIds = projectTemplates.stream()
                .map(ProjectTemplate::getTemplateId)
                .collect(Collectors.toList());
        
        List<Template> templates = templateMapper.selectBatchIds(templateIds);
        List<String> templateNames = templates.stream()
                .filter(t -> t.getDeleted() == 0)
                .map(Template::getTemplateName)
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
                    TemplateProcess firstTemplateProcess = templateProcessMapper.selectOne(new LambdaQueryWrapper<TemplateProcess>()
                            .eq(TemplateProcess::getProcessCatalogId, catalog.getId())
                            .eq(TemplateProcess::getDeleted, 0)
                            .orderByAsc(TemplateProcess::getTemplateId)
                            .orderByAsc(TemplateProcess::getDefaultOrder)
                            .last("LIMIT 1"));
                    
                    if (firstTemplateProcess != null) {
                        option.setTemplateId(firstTemplateProcess.getId());
                        // 查询模板名称
                        Template template = templateMapper.selectById(firstTemplateProcess.getTemplateId());
                        if (template != null) {
                            option.setTemplateName(template.getTemplateName());
                        }
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
        // 获取所有模板
        List<Template> templates = templateMapper.selectList(new LambdaQueryWrapper<Template>()
                .eq(Template::getDeleted, 0));
        
        // 为每个模板获取第一个工序模板的ID和控制时长
        List<TemplateListResponse> templateList = templates.stream()
                .map(template -> {
                    // 获取该模板下的所有工序
                    List<TemplateProcess> templateProcesses = templateProcessMapper.selectList(new LambdaQueryWrapper<TemplateProcess>()
                            .eq(TemplateProcess::getTemplateId, template.getId())
                            .eq(TemplateProcess::getDeleted, 0)
                            .orderByAsc(TemplateProcess::getDefaultOrder));
                    
                    if (templateProcesses.isEmpty()) {
                        return null;
                    }
                    
                    // 获取该模板下第一个工序模板（按defaultOrder排序后的第一个）
                    TemplateProcess firstTemplateProcess = templateProcesses.get(0);
                    // 计算控制时长：模板中所有工序的控制时间总和
                    Integer controlDuration = templateProcesses.stream()
                            .filter(tp -> tp.getControlTime() != null && tp.getControlTime() > 0)
                            .mapToInt(TemplateProcess::getControlTime)
                            .sum();
                    
                    TemplateListResponse response = new TemplateListResponse();
                    response.setTemplateName(template.getTemplateName());
                    response.setTemplateId(firstTemplateProcess.getId());
                    response.setControlDuration(controlDuration);
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
        // 1. 查询该工点关联的所有模板ID
        List<ProjectTemplate> projectTemplates = projectTemplateMapper.selectList(new LambdaQueryWrapper<ProjectTemplate>()
                .eq(ProjectTemplate::getProjectId, siteId)
                .eq(ProjectTemplate::getDeleted, 0));
        
        if (projectTemplates.isEmpty()) {
            return List.of();
        }
        
        // 2. 根据模板ID查询模板信息
        List<Long> templateIds = projectTemplates.stream()
                .map(ProjectTemplate::getTemplateId)
                .collect(Collectors.toList());
        
        List<Template> templates = templateMapper.selectBatchIds(templateIds);
        
        // 3. 为每个模板获取第一个工序模板的ID和控制时长
        List<TemplateListResponse> templateList = templates.stream()
                .filter(t -> t.getDeleted() == 0)
                .map(template -> {
                    // 获取该模板下的所有工序
                    List<TemplateProcess> templateProcesses = templateProcessMapper.selectList(new LambdaQueryWrapper<TemplateProcess>()
                            .eq(TemplateProcess::getTemplateId, template.getId())
                            .eq(TemplateProcess::getDeleted, 0)
                            .orderByAsc(TemplateProcess::getDefaultOrder));
                    
                    if (templateProcesses.isEmpty()) {
                        return null;
                    }
                    
                    // 获取该模板下第一个工序模板（按defaultOrder排序后的第一个）
                    TemplateProcess firstTemplateProcess = templateProcesses.get(0);
                    // 计算控制时长：模板中所有工序的控制时间总和
                    Integer controlDuration = templateProcesses.stream()
                            .filter(tp -> tp.getControlTime() != null && tp.getControlTime() > 0)
                            .mapToInt(TemplateProcess::getControlTime)
                            .sum();
                    
                    TemplateListResponse response = new TemplateListResponse();
                    response.setTemplateName(template.getTemplateName());
                    response.setTemplateId(firstTemplateProcess.getId());
                    response.setControlDuration(controlDuration);
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
        
        // 如果模板名称为空，从关联表查询
        if (resp.getTemplateName() == null && template.getTemplateId() != null) {
            Template t = templateMapper.selectById(template.getTemplateId());
            if (t != null) {
                resp.setTemplateName(t.getTemplateName());
            }
        }
        
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
        log.info("调整同一模板下其他工序模板的顺序，工点ID: {}, 模板名称: {}, 当前模板ID: {}, 旧顺序: {}, 新顺序: {}", 
                siteId, templateName, currentTemplateId, oldOrder, newOrder);
        
        // 1. 根据模板名称查询模板ID
        Template template = templateMapper.selectOne(new LambdaQueryWrapper<Template>()
                .eq(Template::getTemplateName, templateName)
                .eq(Template::getDeleted, 0)
                .last("LIMIT 1"));
        if (template == null) {
            log.warn("模板不存在，模板名称: {}", templateName);
            return;
        }
        
        // 2. 获取同一模板下的所有工序模板（排除当前正在修改的模板）
        List<TemplateProcess> otherTemplateProcesses = templateProcessMapper.selectList(new LambdaQueryWrapper<TemplateProcess>()
                .eq(TemplateProcess::getTemplateId, template.getId())
                .ne(TemplateProcess::getId, currentTemplateId)
                .eq(TemplateProcess::getDeleted, 0)
                .orderByAsc(TemplateProcess::getDefaultOrder));
        
        if (otherTemplateProcesses.isEmpty()) {
            log.debug("没有其他工序模板需要调整，模板ID: {}", template.getId());
            return;
        }
        
        int adjustCount = 0;
        
        // 如果新顺序大于旧顺序（向后移动）
        if (newOrder > oldOrder) {
            // 将顺序在 [oldOrder+1, newOrder] 范围内的其他模板顺序 -1（交换位置）
            for (TemplateProcess otherTemplateProcess : otherTemplateProcesses) {
                Integer otherOrder = otherTemplateProcess.getDefaultOrder();
                if (otherOrder != null && otherOrder > oldOrder && otherOrder <= newOrder) {
                    otherTemplateProcess.setDefaultOrder(otherOrder - 1);
                    templateProcessMapper.updateById(otherTemplateProcess);
                    adjustCount++;
                    log.debug("调整工序模板顺序（向后移动），模板ID: {}, 旧顺序: {}, 新顺序: {}", 
                            otherTemplateProcess.getId(), otherOrder, otherOrder - 1);
                }
            }
        } 
        // 如果新顺序小于旧顺序（向前移动）
        else if (newOrder < oldOrder) {
            // 将顺序在 [newOrder, oldOrder-1] 范围内的其他模板顺序 +1（交换位置）
            for (TemplateProcess otherTemplateProcess : otherTemplateProcesses) {
                Integer otherOrder = otherTemplateProcess.getDefaultOrder();
                if (otherOrder != null && otherOrder >= newOrder && otherOrder < oldOrder) {
                    otherTemplateProcess.setDefaultOrder(otherOrder + 1);
                    templateProcessMapper.updateById(otherTemplateProcess);
                    adjustCount++;
                    log.debug("调整工序模板顺序（向前移动），模板ID: {}, 旧顺序: {}, 新顺序: {}", 
                            otherTemplateProcess.getId(), otherOrder, otherOrder + 1);
                }
            }
        }
        
        log.info("调整同一模板下其他工序模板的顺序完成，模板ID: {}, 调整数量: {}", template.getId(), adjustCount);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessTemplateResponse updateProcessTemplate(Long templateId, UpdateProcessTemplateRequest request) {
        log.info("更新工序模板，模板ID: {}", templateId);
        TemplateProcess templateProcess = templateProcessMapper.selectById(templateId);
        if (templateProcess == null) {
            throw new BusinessException(ResultCode.TEMPLATE_NOT_FOUND);
        }
        
        // 获取模板信息
        Template template = templateMapper.selectById(templateProcess.getTemplateId());
        if (template == null) {
            throw new BusinessException(ResultCode.TEMPLATE_NOT_FOUND);
        }
        
        // 记录旧的默认顺序，用于调整其他模板顺序
        Integer oldDefaultOrder = templateProcess.getDefaultOrder();
        String templateName = template.getTemplateName();
        
        // 如果默认顺序发生变化，需要先调整同一模板下其他工序模板的顺序（避免冲突）
        // 注意：必须先调整其他模板，再更新当前模板，避免顺序冲突
        if (request.getDefaultOrder() != null && oldDefaultOrder != null 
                && !oldDefaultOrder.equals(request.getDefaultOrder())) {
            log.info("工序模板默认顺序发生变化，模板ID: {}, 模板名称: {}, 旧顺序: {}, 新顺序: {}", 
                    templateId, templateName, oldDefaultOrder, request.getDefaultOrder());
            
            // 先调整同一模板下其他工序模板的顺序（为当前模板让出位置）
            // 注意：siteId 参数在新结构中不再需要，但为了保持接口兼容性，传入 null
            adjustOtherTemplatesOrder(null, templateName, templateId, oldDefaultOrder, request.getDefaultOrder());
        }
        
        // 更新当前模板的字段
        if (request.getProcessCatalogId() != null) {
            ProcessCatalog catalog = processCatalogService.getById(request.getProcessCatalogId());
            if (catalog == null) {
                throw new BusinessException("工序字典不存在，ID: " + request.getProcessCatalogId());
            }
            templateProcess.setProcessCatalogId(request.getProcessCatalogId());
        }
        if (request.getControlTime() != null) {
            templateProcess.setControlTime(request.getControlTime());
        }
        if (request.getDefaultOrder() != null) {
            templateProcess.setDefaultOrder(request.getDefaultOrder());
        }
        if (request.getDescription() != null) {
            templateProcess.setDescription(request.getDescription());
        }
        // 最后更新当前模板（包括新的默认顺序）
        templateProcessMapper.updateById(templateProcess);
        
        // 转换为 ProcessTemplate 对象返回
        ProcessTemplate result = BeanUtil.copyProperties(templateProcess, ProcessTemplate.class);
        result.setTemplateName(templateName);
        return convertToResponse(result);
    }
    
    @Override
    public List<com.zzw.zzwgx.dto.response.TemplateWithProcessesResponse> getTemplatesWithProcesses() {
        log.info("查询所有模板及其工序列表");
        
        // 获取所有模板
        List<Template> templates = templateMapper.selectList(new LambdaQueryWrapper<Template>()
                .eq(Template::getDeleted, 0));
        
        // 为每个模板获取其下的所有工序
        List<com.zzw.zzwgx.dto.response.TemplateWithProcessesResponse> result = templates.stream()
                .map(template -> {
                    // 获取该模板下的所有工序模板（按默认顺序排序）
                    List<TemplateProcess> templateProcesses = templateProcessMapper.selectList(new LambdaQueryWrapper<TemplateProcess>()
                            .eq(TemplateProcess::getTemplateId, template.getId())
                            .eq(TemplateProcess::getDeleted, 0)
                            .orderByAsc(TemplateProcess::getDefaultOrder));
                    
                    if (templateProcesses.isEmpty()) {
                        return null;
                    }
                    
                    // 转换为响应对象
                    com.zzw.zzwgx.dto.response.TemplateWithProcessesResponse response = 
                            new com.zzw.zzwgx.dto.response.TemplateWithProcessesResponse();
                    response.setTemplateName(template.getTemplateName());
                    
                    // 获取该模板下第一个工序模板的ID（作为模板ID）
                    TemplateProcess firstTemplateProcess = templateProcesses.get(0);
                    response.setTemplateId(firstTemplateProcess.getId());
                    
                    // 转换所有工序模板为响应对象
                    List<com.zzw.zzwgx.dto.response.ProcessTemplateResponse> processResponses = templateProcesses.stream()
                            .map(tp -> {
                                ProcessTemplate pt = BeanUtil.copyProperties(tp, ProcessTemplate.class);
                                pt.setTemplateName(template.getTemplateName());
                                return convertToResponse(pt);
                            })
                            .collect(Collectors.toList());
                    response.setProcesses(processResponses);
                    
                    return response;
                })
                .filter(template -> template != null)
                .collect(Collectors.toList());
        
        log.info("查询到模板数量: {}", result.size());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindTemplateToProjects(Long templateId, List<Long> projectIds) {
        Template template = templateMapper.selectById(templateId);
        if (template == null || (template.getDeleted() != null && template.getDeleted() == 1)) {
            throw new BusinessException(ResultCode.TEMPLATE_NOT_FOUND);
        }

        List<Long> normalizedIds = normalizeProjectIds(projectIds);

        // 如果未传任何项目，则仅将该模板的所有关联标记为删除
        if (CollectionUtils.isEmpty(normalizedIds)) {
            projectTemplateMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProjectTemplate>()
                    .eq(ProjectTemplate::getTemplateId, templateId)
                    .set(ProjectTemplate::getDeleted, 1));
            log.info("已清空模板的工点关联，模板ID: {}", templateId);
            return;
        }

        // 验证工点存在且类型为 SITE
        List<Project> projects = projectMapper.selectBatchIds(normalizedIds);
        if (projects.size() != normalizedIds.size()) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        boolean hasNonSite = projects.stream().anyMatch(p -> !"SITE".equals(p.getNodeType()));
        if (hasNonSite) {
            throw new BusinessException("仅支持绑定工点（SITE）类型的项目");
        }

        // 去重同一模板的重复关联（保留id最小一条，其余标记删除）
        dedupTemplateProjects(templateId);

        // 将不在目标列表内的关联标记删除
        projectTemplateMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProjectTemplate>()
                .eq(ProjectTemplate::getTemplateId, templateId)
                .notIn(ProjectTemplate::getProjectId, normalizedIds)
                .set(ProjectTemplate::getDeleted, 1));

        // 逐项 upsert（ON DUPLICATE KEY UPDATE deleted=0）
        for (Long projectId : normalizedIds) {
            projectTemplateMapper.upsertProjectTemplate(projectId, templateId);
        }

        log.info("模板绑定工点完成，模板ID: {}, 目标工点数量: {}", templateId, normalizedIds.size());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindProjectToTemplates(Long projectId, List<Long> templateIds) {
        // 校验工点
        Project project = projectMapper.selectById(projectId);
        if (project == null || !"SITE".equals(project.getNodeType())) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }

        // 规范化模板ID列表
        List<Long> normalizedTemplateIds = normalizeProjectIds(templateIds);

        // 如果未传任何模板，则仅将该工点的所有关联标记为删除
        if (CollectionUtils.isEmpty(normalizedTemplateIds)) {
            projectTemplateMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProjectTemplate>()
                    .eq(ProjectTemplate::getProjectId, projectId)
                    .set(ProjectTemplate::getDeleted, 1));
            log.info("已清空工点的模板关联，工点ID: {}", projectId);
            return;
        }

        // 校验模板是否存在且未删除
        List<Template> templates = templateMapper.selectBatchIds(normalizedTemplateIds);
        if (templates.size() != normalizedTemplateIds.size()) {
            throw new BusinessException(ResultCode.TEMPLATE_NOT_FOUND);
        }
        boolean hasDeletedTemplate = templates.stream()
                .anyMatch(t -> t.getDeleted() != null && t.getDeleted() == 1);
        if (hasDeletedTemplate) {
            throw new BusinessException(ResultCode.TEMPLATE_NOT_FOUND);
        }

        // 去重该工点下的重复关联（保留id最小一条，其余标记删除）
        dedupProjectTemplates(projectId);

        // 将不在目标模板列表内的关联标记删除
        projectTemplateMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProjectTemplate>()
                .eq(ProjectTemplate::getProjectId, projectId)
                .notIn(ProjectTemplate::getTemplateId, normalizedTemplateIds)
                .set(ProjectTemplate::getDeleted, 1));

        // 逐项 upsert（ON DUPLICATE KEY UPDATE deleted=0）
        for (Long templateId : normalizedTemplateIds) {
            projectTemplateMapper.upsertProjectTemplate(projectId, templateId);
        }

        log.info("工点绑定模板完成，工点ID: {}, 目标模板数量: {}", projectId, normalizedTemplateIds.size());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessTemplateResponse createProcessTemplate(CreateProcessTemplateRequest request) {
        log.info("创建工序模板，工点ID: {}, 模板名称: {}, 工序字典ID: {}", request.getSiteId(), request.getTemplateName(), request.getProcessCatalogId());
        
        // 验证工点是否存在且类型为 SITE
        Project site = projectMapper.selectById(request.getSiteId());
        if (site == null || !"SITE".equals(site.getNodeType())) {
            throw new BusinessException("工点不存在或不是工点类型，ID: " + request.getSiteId());
        }
        
        ProcessCatalog catalog = processCatalogService.getById(request.getProcessCatalogId());
        if (catalog == null) {
            throw new BusinessException("工序字典不存在，ID: " + request.getProcessCatalogId());
        }
        
        // 1. 检查或创建模板（template表）
        Template template = templateMapper.selectOne(new LambdaQueryWrapper<Template>()
                .eq(Template::getTemplateName, request.getTemplateName())
                .eq(Template::getDeleted, 0)
                .last("LIMIT 1"));
        if (template == null) {
            template = new Template();
            template.setTemplateName(request.getTemplateName());
            templateMapper.insert(template);
            log.info("创建新模板，模板ID: {}, 模板名称: {}", template.getId(), template.getTemplateName());
        }
        
        // 2. 检查或创建工点-模板关联（project_template表）
        ProjectTemplate projectTemplate = projectTemplateMapper.selectOne(new LambdaQueryWrapper<ProjectTemplate>()
                .eq(ProjectTemplate::getProjectId, request.getSiteId())
                .eq(ProjectTemplate::getTemplateId, template.getId())
                .eq(ProjectTemplate::getDeleted, 0)
                .last("LIMIT 1"));
        if (projectTemplate == null) {
            projectTemplate = new ProjectTemplate();
            projectTemplate.setProjectId(request.getSiteId());
            projectTemplate.setTemplateId(template.getId());
            projectTemplateMapper.insert(projectTemplate);
            log.info("创建工点-模板关联，工点ID: {}, 模板ID: {}", request.getSiteId(), template.getId());
        }
        
        // 3. 检查是否存在重复的模板-工序记录（同一模板、同一顺序）
        TemplateProcess existingTemplateProcess = templateProcessMapper.selectOne(new LambdaQueryWrapper<TemplateProcess>()
                .eq(TemplateProcess::getTemplateId, template.getId())
                .eq(TemplateProcess::getDefaultOrder, request.getDefaultOrder())
                .eq(TemplateProcess::getDeleted, 0)
                .last("LIMIT 1"));
        if (existingTemplateProcess != null) {
            throw new BusinessException(ResultCode.TEMPLATE_DUPLICATE.getCode(), 
                    String.format("模板\"%s\"中顺序%d已被占用，请使用其他顺序", 
                            request.getTemplateName(), request.getDefaultOrder()));
        }
        
        // 4. 创建模板-工序关联（template_process表）
        TemplateProcess templateProcess = new TemplateProcess();
        templateProcess.setTemplateId(template.getId());
        templateProcess.setProcessCatalogId(request.getProcessCatalogId());
        templateProcess.setControlTime(request.getControlTime());
        templateProcess.setDefaultOrder(request.getDefaultOrder());
        templateProcess.setDescription(request.getDescription());
        templateProcessMapper.insert(templateProcess);
        
        // 5. 转换为 ProcessTemplate 对象返回（为了兼容接口）
        ProcessTemplate result = new ProcessTemplate();
        result.setId(templateProcess.getId());
        result.setTemplateId(template.getId());
        result.setTemplateName(request.getTemplateName());
        result.setSiteId(request.getSiteId());
        result.setProcessCatalogId(request.getProcessCatalogId());
        result.setControlTime(request.getControlTime());
        result.setDefaultOrder(request.getDefaultOrder());
        result.setDescription(request.getDescription());
        
        return convertToResponse(result);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ProcessTemplateResponse> createProcessTemplatesBatch(CreateProcessTemplateBatchRequest request) {
        log.info("批量创建工序模板，工点ID: {}, 模板名称: {}, 工序数量: {}", request.getSiteId(), request.getTemplateName(), request.getProcesses().size());
        
        // 验证工点是否存在且类型为 SITE
        Project site = projectMapper.selectById(request.getSiteId());
        if (site == null || !"SITE".equals(site.getNodeType())) {
            throw new BusinessException("工点不存在或不是工点类型，ID: " + request.getSiteId());
        }
        
        // 1. 检查或创建模板（template表）
        Template template = templateMapper.selectOne(new LambdaQueryWrapper<Template>()
                .eq(Template::getTemplateName, request.getTemplateName())
                .eq(Template::getDeleted, 0)
                .last("LIMIT 1"));
        if (template == null) {
            template = new Template();
            template.setTemplateName(request.getTemplateName());
            templateMapper.insert(template);
            log.info("创建新模板，模板ID: {}, 模板名称: {}", template.getId(), template.getTemplateName());
        }
        
        // 2. 检查或创建工点-模板关联（project_template表）
        ProjectTemplate projectTemplate = projectTemplateMapper.selectOne(new LambdaQueryWrapper<ProjectTemplate>()
                .eq(ProjectTemplate::getProjectId, request.getSiteId())
                .eq(ProjectTemplate::getTemplateId, template.getId())
                .eq(ProjectTemplate::getDeleted, 0)
                .last("LIMIT 1"));
        if (projectTemplate == null) {
            projectTemplate = new ProjectTemplate();
            projectTemplate.setProjectId(request.getSiteId());
            projectTemplate.setTemplateId(template.getId());
            projectTemplateMapper.insert(projectTemplate);
            log.info("创建工点-模板关联，工点ID: {}, 模板ID: {}", request.getSiteId(), template.getId());
        }
        
        // 3. 先检查是否存在重复的模板-工序记录（同一模板、同一顺序）
        for (CreateProcessTemplateBatchRequest.Item item : request.getProcesses()) {
            TemplateProcess existingTemplateProcess = templateProcessMapper.selectOne(new LambdaQueryWrapper<TemplateProcess>()
                    .eq(TemplateProcess::getTemplateId, template.getId())
                    .eq(TemplateProcess::getDefaultOrder, item.getDefaultOrder())
                    .eq(TemplateProcess::getDeleted, 0)
                    .last("LIMIT 1"));
            if (existingTemplateProcess != null) {
                throw new BusinessException(ResultCode.TEMPLATE_DUPLICATE.getCode(), 
                        String.format("模板\"%s\"中顺序%d已被占用，请使用其他顺序", 
                                request.getTemplateName(), item.getDefaultOrder()));
            }
        }
        
        // 4. 批量创建模板-工序关联（template_process表）
        List<TemplateProcess> templateProcesses = new java.util.ArrayList<>();
        for (CreateProcessTemplateBatchRequest.Item item : request.getProcesses()) {
            ProcessCatalog catalog = processCatalogService.getById(item.getProcessCatalogId());
            if (catalog == null) {
                throw new BusinessException("工序字典不存在，ID: " + item.getProcessCatalogId());
            }
            TemplateProcess templateProcess = new TemplateProcess();
            templateProcess.setTemplateId(template.getId());
            templateProcess.setProcessCatalogId(item.getProcessCatalogId());
            templateProcess.setControlTime(item.getControlTime());
            templateProcess.setDefaultOrder(item.getDefaultOrder());
            templateProcess.setDescription(item.getDescription());
            templateProcesses.add(templateProcess);
        }
        // 批量插入模板-工序关联
        for (TemplateProcess tp : templateProcesses) {
            templateProcessMapper.insert(tp);
        }
        
        // 5. 转换为 ProcessTemplate 对象返回（为了兼容接口）
        return templateProcesses.stream()
                .map(tp -> {
                    ProcessTemplate pt = new ProcessTemplate();
                    pt.setId(tp.getId());
                    pt.setTemplateId(tp.getTemplateId());
                    pt.setTemplateName(request.getTemplateName());
                    pt.setSiteId(request.getSiteId());
                    pt.setProcessCatalogId(tp.getProcessCatalogId());
                    pt.setControlTime(tp.getControlTime());
                    pt.setDefaultOrder(tp.getDefaultOrder());
                    pt.setDescription(tp.getDescription());
                    return convertToResponse(pt);
                })
                .collect(Collectors.toList());
    }

    /**
     * 去重：同一模板同一工点如有多条记录，仅保留id最小的一条，其余标记为删除
     */
    private void dedupTemplateProjects(Long templateId) {
        List<ProjectTemplate> relations = projectTemplateMapper.selectList(new LambdaQueryWrapper<ProjectTemplate>()
                .eq(ProjectTemplate::getTemplateId, templateId));
        if (CollectionUtils.isEmpty(relations)) {
            return;
        }
        Map<Long, List<ProjectTemplate>> grouped = relations.stream()
                .filter(pt -> pt.getProjectId() != null)
                .collect(Collectors.groupingBy(ProjectTemplate::getProjectId));

        List<ProjectTemplate> toUpdate = grouped.values().stream()
                .filter(list -> list.size() > 1)
                .flatMap(list -> {
                    List<ProjectTemplate> sorted = list.stream()
                            .sorted(Comparator.comparing(ProjectTemplate::getId, Comparator.nullsLast(Long::compareTo)))
                            .toList();
                    return sorted.stream().skip(1).peek(pt -> pt.setDeleted(1));
                })
                .collect(Collectors.toList());

        if (!toUpdate.isEmpty()) {
            toUpdate.forEach(projectTemplateMapper::updateById);
            log.info("去重模板-工点关联，模板ID: {}, 处理条数: {}", templateId, toUpdate.size());
        }
    }

    /**
     * 去重：同一工点同一模板如有多条记录，仅保留id最小的一条，其余标记为删除
     */
    private void dedupProjectTemplates(Long projectId) {
        List<ProjectTemplate> relations = projectTemplateMapper.selectList(new LambdaQueryWrapper<ProjectTemplate>()
                .eq(ProjectTemplate::getProjectId, projectId));
        if (CollectionUtils.isEmpty(relations)) {
            return;
        }
        Map<Long, List<ProjectTemplate>> grouped = relations.stream()
                .filter(pt -> pt.getTemplateId() != null)
                .collect(Collectors.groupingBy(ProjectTemplate::getTemplateId));

        List<ProjectTemplate> toUpdate = grouped.values().stream()
                .filter(list -> list.size() > 1)
                .flatMap(list -> {
                    List<ProjectTemplate> sorted = list.stream()
                            .sorted(Comparator.comparing(ProjectTemplate::getId, Comparator.nullsLast(Long::compareTo)))
                            .toList();
                    return sorted.stream().skip(1).peek(pt -> pt.setDeleted(1));
                })
                .collect(Collectors.toList());

        if (!toUpdate.isEmpty()) {
            toUpdate.forEach(projectTemplateMapper::updateById);
            log.info("去重工点-模板关联，工点ID: {}, 处理条数: {}", projectId, toUpdate.size());
        }
    }

    /**
     * 规范化工点ID列表：去空、去重、保持顺序
     */
    private List<Long> normalizeProjectIds(List<Long> projectIds) {
        if (CollectionUtils.isEmpty(projectIds)) {
            return List.of();
        }
        return projectIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .collect(Collectors.toList());
    }
}

