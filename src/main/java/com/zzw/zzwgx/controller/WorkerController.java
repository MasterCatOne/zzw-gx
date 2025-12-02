package com.zzw.zzwgx.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzw.zzwgx.common.Result;
import com.zzw.zzwgx.dto.request.CompleteTaskRequest;
import com.zzw.zzwgx.dto.response.TaskDetailResponse;
import com.zzw.zzwgx.dto.response.TaskListResponse;
import com.zzw.zzwgx.dto.response.UserProfileResponse;
import com.zzw.zzwgx.security.SecurityUtils;
import com.zzw.zzwgx.service.TaskService;
import com.zzw.zzwgx.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 施工人员控制器
 */
@Slf4j
@Tag(name = "施工人员管理", description = "施工人员相关接口")
@RestController
@RequestMapping("/api/worker")
@RequiredArgsConstructor
public class WorkerController {
    
    private final TaskService taskService;
    private final UserService userService;
    
    @Operation(summary = "获取任务列表", description = "分页查询当前施工人员的任务列表")
    @GetMapping("/tasks")
    public Result<Page<TaskListResponse>> getTasks(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String projectName) {
        Long workerId = SecurityUtils.getCurrentUserId();
        log.info("查询任务列表，施工人员ID: {}, 页码: {}, 每页大小: {}, 项目名称: {}", workerId, pageNum, pageSize, projectName);
        Page<TaskListResponse> response = taskService.getTaskList(workerId, pageNum, pageSize, projectName);
        return Result.success(response);
    }
    
    @Operation(summary = "获取任务详情", description = "获取指定任务的详细信息")
    @GetMapping("/tasks/{taskId}")
    public Result<TaskDetailResponse> getTaskDetail(@PathVariable Long taskId) {
        Long workerId = SecurityUtils.getCurrentUserId();
        log.info("查询任务详情，任务ID: {}, 施工人员ID: {}", taskId, workerId);
        TaskDetailResponse response = taskService.getTaskDetail(taskId, workerId);
        return Result.success(response);
    }
    
    @Operation(summary = "开始任务", description = "开始执行指定任务")
    @PostMapping("/tasks/{taskId}/start")
    public Result<?> startTask(@PathVariable Long taskId) {
        Long workerId = SecurityUtils.getCurrentUserId();
        log.info("开始任务，任务ID: {}, 施工人员ID: {}", taskId, workerId);
        taskService.startTask(taskId, workerId);
        return Result.success();
    }
    
    @Operation(summary = "完成任务", description = "完成指定任务，如果超时需要填写超时原因")
    @PostMapping("/tasks/{taskId}/complete")
    public Result<?> completeTask(@PathVariable Long taskId, @RequestBody CompleteTaskRequest request) {
        Long workerId = SecurityUtils.getCurrentUserId();
        log.info("完成任务，任务ID: {}, 施工人员ID: {}, 是否有超时原因: {}", 
                taskId, workerId, request.getOvertimeReason() != null);
        taskService.completeTask(taskId, workerId, request);
        return Result.success();
    }
    
    @Operation(summary = "获取个人信息", description = "获取当前登录用户的个人信息")
    @GetMapping("/profile")
    public Result<UserProfileResponse> getProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("查询个人信息，用户ID: {}", userId);
        UserProfileResponse response = userService.getProfile(userId);
        return Result.success(response);
    }
}

