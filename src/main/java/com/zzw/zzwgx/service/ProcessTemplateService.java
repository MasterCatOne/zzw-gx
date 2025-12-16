package com.zzw.zzwgx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzw.zzwgx.dto.response.ProcessTemplateOptionResponse;
import com.zzw.zzwgx.dto.response.ProcessTemplateResponse;
import com.zzw.zzwgx.dto.response.TemplateListResponse;
import com.zzw.zzwgx.entity.ProcessTemplate;

import java.util.List;

/**
 * 工序模板服务接口
 */
public interface ProcessTemplateService extends IService<ProcessTemplate> {
    
    /**
     * 根据模板名称获取工序模板列表
     */
    List<ProcessTemplate> getTemplatesByName(String templateName);
    
    /**
     * 根据工点ID和模板名称获取工序模板列表
     */
    List<ProcessTemplate> getTemplatesByNameAndSiteId(String templateName, Long siteId);
    
    /**
     * 根据模板ID获取工序模板列表
     */
    List<ProcessTemplate> getTemplatesByTemplateId(Long templateId);
    
    /**
     * 获取所有模板名称列表（去重）
     */
    List<String> getAllTemplateNames();
    
    /**
     * 根据工点ID获取该工点下的所有模板名称列表（去重）
     */
    List<String> getTemplateNamesBySiteId(Long siteId);
    
    /**
     * 获取所有工序模板选项列表（用于前端下拉选择）
     */
    List<ProcessTemplateOptionResponse> getAllProcessTemplateOptions();
    
    /**
     * 获取模板列表（用于前端选择模板，每个模板包含名称和第一个工序模板的ID）
     */
    List<TemplateListResponse> getTemplateList();
    
    /**
     * 根据工点ID获取该工点下的模板列表（用于前端选择模板，每个模板包含名称和第一个工序模板的ID）
     */
    List<TemplateListResponse> getTemplateListBySiteId(Long siteId);
    
    /**
     * 转换ProcessTemplate为ProcessTemplateResponse
     */
    ProcessTemplateResponse convertToResponse(ProcessTemplate template);

    /**
     * 按工点绑定模板列表（SITE），传入目标模板列表，支持幂等更新
     */
    void bindProjectToTemplates(Long projectId, List<Long> templateIds);
    
    /**
     * 调整同一工点下同一模板名称下其他工序模板的顺序
     * 当某个工序模板的 defaultOrder 发生变化时，需要调整其他模板的顺序以避免冲突
     * 
     * @param siteId 工点ID
     * @param templateName 模板名称
     * @param currentTemplateId 当前正在修改的模板ID（排除自己）
     * @param oldOrder 旧的顺序
     * @param newOrder 新的顺序
     */
    void adjustOtherTemplatesOrder(Long siteId, String templateName, Long currentTemplateId, Integer oldOrder, Integer newOrder);
    
    /**
     * 更新工序模板
     * 支持部分字段更新，如果默认顺序发生变化，会自动调整同一模板名称下其他工序模板的顺序
     * 
     * @param templateId 模板ID
     * @param request 更新请求
     * @return 更新后的工序模板响应
     */
    ProcessTemplateResponse updateProcessTemplate(Long templateId, com.zzw.zzwgx.dto.request.UpdateProcessTemplateRequest request);
    
    /**
     * 获取所有模板及其工序列表
     * 返回所有模板，每个模板包含其下的所有工序（按默认顺序排序）
     * 
     * @return 模板及其工序列表
     */
    List<com.zzw.zzwgx.dto.response.TemplateWithProcessesResponse> getTemplatesWithProcesses();
    
    /**
     * 创建工序模板
     * 如果模板不存在则创建，如果工点-模板关联不存在则创建关联，最后创建模板-工序关联
     * 
     * @param request 创建请求
     * @return 创建后的工序模板响应
     */
    ProcessTemplateResponse createProcessTemplate(com.zzw.zzwgx.dto.request.CreateProcessTemplateRequest request);
    
    /**
     * 批量创建工序模板
     * 一次性为同一个模板名称创建多条工序模板，避免逐条新增
     * 
     * @param request 批量创建请求
     * @return 创建后的工序模板响应列表
     */
    List<ProcessTemplateResponse> createProcessTemplatesBatch(com.zzw.zzwgx.dto.request.CreateProcessTemplateBatchRequest request);
}

