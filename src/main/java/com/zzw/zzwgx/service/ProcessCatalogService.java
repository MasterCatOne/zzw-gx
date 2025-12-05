package com.zzw.zzwgx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzw.zzwgx.dto.request.CreateProcessCatalogRequest;
import com.zzw.zzwgx.dto.request.UpdateProcessCatalogOrderRequest;
import com.zzw.zzwgx.dto.request.UpdateProcessCatalogRequest;
import com.zzw.zzwgx.dto.response.ProcessCatalogResponse;
import com.zzw.zzwgx.entity.ProcessCatalog;

import java.util.List;

/**
 * 工序字典服务接口
 */
public interface ProcessCatalogService extends IService<ProcessCatalog> {
    
    /**
     * 获取所有工序字典列表（按显示顺序排序）
     */
    List<ProcessCatalogResponse> getAllProcessCatalogs();
    
    /**
     * 创建工序字典
     */
    ProcessCatalogResponse createProcessCatalog(CreateProcessCatalogRequest request);
    
    /**
     * 更新工序字典
     */
    ProcessCatalogResponse updateProcessCatalog(Long catalogId, UpdateProcessCatalogRequest request);
    
    /**
     * 批量更新工序顺序
     */
    void updateProcessCatalogOrder(UpdateProcessCatalogOrderRequest request);
}

