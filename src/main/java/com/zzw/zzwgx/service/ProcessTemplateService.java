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
     * 根据模板ID获取工序模板列表
     */
    List<ProcessTemplate> getTemplatesByTemplateId(Long templateId);
    
    /**
     * 获取所有模板名称列表（去重）
     */
    List<String> getAllTemplateNames();
    
    /**
     * 获取所有工序模板选项列表（用于前端下拉选择）
     */
    List<ProcessTemplateOptionResponse> getAllProcessTemplateOptions();
    
    /**
     * 获取模板列表（用于前端选择模板，每个模板包含名称和第一个工序模板的ID）
     */
    List<TemplateListResponse> getTemplateList();
    
    /**
     * 转换ProcessTemplate为ProcessTemplateResponse
     */
    ProcessTemplateResponse convertToResponse(ProcessTemplate template);
}

