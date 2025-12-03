package com.zzw.zzwgx.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zzw.zzwgx.dto.request.CreateCycleRequest;
import com.zzw.zzwgx.dto.request.UpdateCycleRequest;
import com.zzw.zzwgx.dto.response.CycleResponse;
import com.zzw.zzwgx.entity.Cycle;

/**
 * 循环服务接口
 */
public interface CycleService extends IService<Cycle> {
    
    /**
     * 创建循环
     */
    CycleResponse createCycle(CreateCycleRequest request);
    
    /**
     * 更新循环
     */
    CycleResponse updateCycle(Long cycleId, UpdateCycleRequest request);
    
    /**
     * 获取循环详情
     */
    CycleResponse getCycleDetail(Long cycleId);
    
    /**
     * 分页查询项目下的循环
     */
    Page<CycleResponse> getCyclesByProject(Long projectId, Integer pageNum, Integer pageSize);
    
    /**
     * 根据项目ID获取当前循环
     */
    Cycle getCurrentCycleByProjectId(Long projectId);
    
    /**
     * 根据项目ID获取最新循环
     */
    Cycle getLatestCycleByProjectId(Long projectId);

    /**
     * 根据项目ID和循环号获取循环
     */
    Cycle getCycleByProjectAndNumber(Long projectId, Integer cycleNumber);
}

