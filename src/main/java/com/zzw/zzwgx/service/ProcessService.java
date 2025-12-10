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
     * 创建并立即开工的工序（状态为进行中，必填实际开始时间）
     */
    ProcessResponse createProcessAndStart(CreateProcessRequest request);
    
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
     * 施工人员开始工序（返回提示信息，不阻止开始）
     */
    com.zzw.zzwgx.dto.response.StartProcessResponse startWorkerProcess(Long processId, Long workerId, java.time.LocalDateTime actualStartTime);
    
    /**
     * 计算循环的总工序时间（考虑重叠时间不重复计算）
     */
    com.zzw.zzwgx.dto.response.CycleProcessTimeResponse calculateCycleProcessTime(Long cycleId);

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
    
    /**
     * 施工人员填报超时原因（可在循环完成前填报）
     */
    void submitOvertimeReason(Long processId, Long workerId, String overtimeReason);
    
    /**
     * 查询超时未填报原因的工序列表（管理员查看，仅返回循环未完成的工序）
     */
    Page<com.zzw.zzwgx.dto.response.OvertimeProcessResponse> getOvertimeProcessesWithoutReason(
            Integer pageNum, Integer pageSize, String projectName);

    /**
     * 施工人员完成工序（自动填充实际结束时间为当前时间并标记完成，不检查循环状态）
     */
    ProcessResponse completeWorkerProcess(Long processId, Long workerId);
    
    /**
     * 施工人员完成工序并检查循环状态（如果所有工序都完成，则更新循环状态为已完成）
     */
    ProcessResponse completeWorkerProcessAndCheckCycle(Long processId, Long workerId);
}

