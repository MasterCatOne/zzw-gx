package com.zzw.zzwgx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzw.zzwgx.dto.request.CreateProcessRequest;
import com.zzw.zzwgx.dto.response.ProcessDetailResponse;
import com.zzw.zzwgx.dto.response.ProcessResponse;
import com.zzw.zzwgx.entity.Process;

import java.util.List;

/**
 * 工序服务接口
 */
public interface ProcessService extends IService<Process> {
    
    /**
     * 创建工序
     */
    ProcessResponse createProcess(CreateProcessRequest request);
    
    /**
     * 根据循环ID获取工序列表
     */
    List<Process> getProcessesByCycleId(Long cycleId);
    
    /**
     * 根据循环ID和顺序获取上一个工序
     */
    Process getPreviousProcess(Long cycleId, Integer startOrder);
    
    /**
     * 获取工序详情
     */
    ProcessDetailResponse getProcessDetail(Long processId);
}

