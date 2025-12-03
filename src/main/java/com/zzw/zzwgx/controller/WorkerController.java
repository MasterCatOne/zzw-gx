package com.zzw.zzwgx.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzw.zzwgx.common.Result;
import com.zzw.zzwgx.dto.request.CompleteTaskRequest;
import com.zzw.zzwgx.dto.response.TaskDetailResponse;
import com.zzw.zzwgx.dto.response.TaskListResponse;
import com.zzw.zzwgx.dto.response.UserProfileResponse;
import com.zzw.zzwgx.security.SecurityUtils;
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

    private final UserService userService;
    @Operation(summary = "获取个人信息", description = "获取当前登录用户的个人信息")
    @GetMapping("/profile")
    public Result<UserProfileResponse> getProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("查询个人信息，用户ID: {}", userId);
        UserProfileResponse response = userService.getProfile(userId);
        return Result.success(response);
    }
}

