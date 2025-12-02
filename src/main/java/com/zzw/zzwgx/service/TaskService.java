package com.zzw.zzwgx.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zzw.zzwgx.dto.request.CompleteTaskRequest;
import com.zzw.zzwgx.dto.response.TaskDetailResponse;
import com.zzw.zzwgx.dto.response.TaskListResponse;
import com.zzw.zzwgx.entity.Task;

/**
 * 任务服务接口
 */
public interface TaskService extends IService<Task> {
    
    /**
     * 分页查询施工人员的任务列表
     */
    Page<Task> getTaskPageByWorkerId(Long workerId, Integer pageNum, Integer pageSize, String projectName);
    
    /**
     * 获取任务列表（返回Response）
     */
    Page<TaskListResponse> getTaskList(Long workerId, Integer pageNum, Integer pageSize, String projectName);
    
    /**
     * 获取任务详情
     */
    TaskDetailResponse getTaskDetail(Long taskId, Long workerId);
    
    /**
     * 开始任务
     */
    void startTask(Long taskId, Long workerId);
    
    /**
     * 完成任务
     */
    void completeTask(Long taskId, Long workerId, CompleteTaskRequest request);
}

