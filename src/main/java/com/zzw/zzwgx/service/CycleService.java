package com.zzw.zzwgx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzw.zzwgx.dto.request.CreateCycleRequest;
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
     * 根据项目ID获取当前循环
     */
    Cycle getCurrentCycleByProjectId(Long projectId);
    
    /**
     * 根据项目ID获取最新循环
     */
    Cycle getLatestCycleByProjectId(Long projectId);
}

