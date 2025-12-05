package com.zzw.zzwgx.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zzw.zzwgx.dto.request.CreateProcessRequest;
import com.zzw.zzwgx.dto.request.UpdateProcessOrderRequest;
import com.zzw.zzwgx.dto.request.UpdateProcessRequest;
import com.zzw.zzwgx.dto.response.ProcessDetailResponse;
import com.zzw.zzwgx.dto.response.ProcessResponse;
import com.zzw.zzwgx.dto.response.WorkerProcessListResponse;
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

    /**
     * 施工人员查看自己的工序详情
     */
    ProcessDetailResponse getWorkerProcessDetail(Long processId, Long workerId);

    /**
     * 施工人员开始工序
     */
    void startWorkerProcess(Long processId, Long workerId, java.time.LocalDateTime actualStartTime);

    /**
     * 更新工序
     */
    ProcessResponse updateProcess(Long processId, UpdateProcessRequest request);

    /**
     * 施工人员工序列表
     *
     * @param workerId    当前施工人员用户ID
     * @param pageNum     页码
     * @param pageSize    每页数量
     * @param projectName 工点名称（模糊查询）
     * @param status      工序状态：NOT_STARTED/IN_PROGRESS/COMPLETED
     */
    Page<WorkerProcessListResponse> getWorkerProcessList(Long workerId,
                                                         Integer pageNum,
                                                         Integer pageSize,
                                                         String projectName,
                                                         String status);
    
    /**
     * 批量更新工序顺序
     */
    void updateProcessOrders(Long cycleId, UpdateProcessOrderRequest request);
}

