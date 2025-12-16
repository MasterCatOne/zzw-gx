package com.zzw.zzwgx.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzw.zzwgx.common.Result;
import com.zzw.zzwgx.dto.request.CreateUserRequest;
import com.zzw.zzwgx.dto.request.UpdateUserRequest;
import com.zzw.zzwgx.dto.response.UserListResponse;
import com.zzw.zzwgx.dto.response.UserViewListResponse;
import com.zzw.zzwgx.entity.User;
import com.zzw.zzwgx.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @Operation(summary = "获取用户列表", description = "管理员分页查询用户列表，支持按用户名、姓名、角色搜索。用于管理员查看和管理所有用户账号。", tags = {"管理员管理-用户管理"})
    @GetMapping("/users")
    public Result<Page<UserListResponse>> getUserList(
            @Parameter(description = "页码，从1开始", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页记录数", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "用户名关键词，支持模糊搜索", example = "worker") @RequestParam(required = false) String username,
            @Parameter(description = "姓名关键词，支持模糊搜索", example = "张三") @RequestParam(required = false) String realName,
            @Parameter(description = "角色代码：WORKER/ADMIN/SYSTEM_ADMIN", example = "WORKER") @RequestParam(required = false) String roleCode) {
        log.info("管理员查询用户列表，页码: {}, 每页大小: {}, 用户名: {}, 姓名: {}, 角色: {}", 
                pageNum, pageSize, username, realName, roleCode);
        Page<UserListResponse> page = userService.getUserList(pageNum, pageSize, username, realName, roleCode);
        return Result.success(page);
    }
    
    @Operation(summary = "获取施工人员列表", description = "供项目管理员选择施工人员使用，只返回角色为WORKER的用户，可按用户名或姓名模糊搜索，可选按项目过滤。", tags = {"管理员管理-用户管理"})
    @GetMapping("/workers")
    public Result<List<UserViewListResponse>> listWorkers(
            @Parameter(description = "项目ID（可选，传入则仅返回该项目已有参与记录的施工人员）", example = "7") @RequestParam(required = false) Long projectId,
            @Parameter(description = "用户名或姓名关键词", example = "张") @RequestParam(required = false) String keyword) {
        List<UserViewListResponse> workers = userService.listWorkers(projectId, keyword);
        return Result.success(workers);
    }
    
    @Operation(summary = "创建用户账号", description = "管理员为填报人员创建账号。每个填报人员固定一个账号，便于区分。可以指定账号、密码、姓名、身份证号、手机号、角色等信息。创建用户时可同时分配工点或隧道（通过siteIds和tunnelIds字段），创建用户和分配项目在同一事务中执行，确保数据一致性。", tags = {"管理员管理-用户管理"})
    @PostMapping("/users")
    public Result<UserListResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("管理员创建用户账号，用户名: {}, 姓名: {}, 角色: {}, 工点IDs: {}, 隧道IDs: {}", 
                request.getUsername(), request.getRealName(), request.getRoleCode(), 
                request.getSiteIds(), request.getTunnelIds());
        User user = userService.createUser(request);
        
        // 转换为响应DTO
        UserListResponse response = new UserListResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setIdCard(user.getIdCard());
        response.setPhone(user.getPhone());
        response.setStatus(user.getStatus());
        response.setRoles(userService.getUserRoleCodes(user.getId()));
        response.setCreateTime(user.getCreateTime());
        response.setUpdateTime(user.getUpdateTime());
        return Result.success(response);
    }
    
    @Operation(summary = "修改用户账号", description = "管理员修改用户账号信息。可以修改姓名、身份证号、手机号、密码、状态、角色等信息。更新用户时可同时修改绑定的工点或隧道（通过siteIds和tunnelIds字段），更新用户和分配项目在同一事务中执行，确保数据一致性。如果提供了siteIds或tunnelIds，将替换用户原有的工点和隧道绑定。", tags = {"管理员管理-用户管理"})
    @PutMapping("/users/{userId}")
    public Result<UserListResponse> updateUser(
            @Parameter(description = "用户ID", required = true, example = "1") @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request) {
        log.info("管理员修改用户账号，用户ID: {}, 工点IDs: {}, 隧道IDs: {}", 
                userId, request.getSiteIds(), request.getTunnelIds());
        User user = userService.updateUser(userId, request);

        // 转换为响应DTO
        UserListResponse response = new UserListResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setIdCard(user.getIdCard());
        response.setPhone(user.getPhone());
        response.setStatus(user.getStatus());
        response.setRoles(userService.getUserRoleCodes(user.getId()));
        response.setCreateTime(user.getCreateTime());
        response.setUpdateTime(user.getUpdateTime());
        return Result.success(response);
    }
}

