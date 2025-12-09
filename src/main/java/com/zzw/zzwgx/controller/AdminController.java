package com.zzw.zzwgx.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzw.zzwgx.common.Result;
import com.zzw.zzwgx.dto.request.CreateCycleRequest;
import com.zzw.zzwgx.dto.request.CreateProcessRequest;
import com.zzw.zzwgx.dto.request.CreateProcessTemplateRequest;
import com.zzw.zzwgx.dto.request.CreateProcessCatalogRequest;
import com.zzw.zzwgx.dto.request.CreateUserRequest;
import com.zzw.zzwgx.dto.request.ProjectRequest;
import com.zzw.zzwgx.dto.request.UpdateProcessCatalogOrderRequest;
import com.zzw.zzwgx.dto.request.UpdateProcessCatalogRequest;
import com.zzw.zzwgx.dto.request.UpdateProcessOrderRequest;
import com.zzw.zzwgx.dto.request.UpdateProcessRequest;
import com.zzw.zzwgx.dto.request.UpdateProcessTemplateRequest;
import com.zzw.zzwgx.dto.request.UpdateCycleRequest;
import com.zzw.zzwgx.dto.request.UpdateUserRequest;
import com.zzw.zzwgx.dto.response.*;
import com.zzw.zzwgx.service.CycleService;
import com.zzw.zzwgx.service.ProcessCatalogService;
import com.zzw.zzwgx.service.ProcessService;
import com.zzw.zzwgx.service.ProjectService;
import com.zzw.zzwgx.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 管理员控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final ProjectService projectService;
    private final CycleService cycleService;
    private final ProcessService processService;
    private final com.zzw.zzwgx.service.ProcessTemplateService processTemplateService;
    private final ProcessCatalogService processCatalogService;
    private final UserService userService;
    
    @Operation(summary = "获取工点列表", description = "分页查询工点列表，支持按名称和状态搜索。响应数据包含工点ID、名称、状态、当前循环次数、围岩等级等信息。当前阶段为了方便前端联调，可选传入用户ID进行权限过滤；正式环境建议通过登录token自动识别用户。", tags = {"管理员管理-工点管理"})
    @GetMapping("/projects")
    public Result<Page<ProjectListResponse>> getProjects(
            @Parameter(description = "页码，从1开始", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页记录数", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "工点名称关键词，支持模糊搜索", example = "工点1") @RequestParam(required = false) String name,
            @Parameter(description = "工点状态：IN_PROGRESS/COMPLETED/PAUSED", example = "IN_PROGRESS") @RequestParam(required = false) String status,
            @Parameter(description = "用户ID（测试/联调用，可选）。如果不传，则根据当前登录用户的token进行权限控制；如果传入，则按该用户的工点权限进行过滤。", example = "2") @RequestParam(required = false) Long userId) {
        log.info("查询工点列表，页码: {}, 每页大小: {}, 名称关键词: {}, 状态: {}, 指定用户ID: {}", pageNum, pageSize, name, status, userId);
        Page<ProjectListResponse> response = projectService.getProjectList(pageNum, pageSize, name, status, userId);
        return Result.success(response);
    }
    
    @Operation(summary = "创建工点信息", description = "管理员创建工点信息。需要指定父节点ID（如果为顶级节点则传null）、节点类型（PROJECT/SECTION/TUNNEL/SITE）、节点名称、编号等信息。", tags = {"管理员管理-工点管理"})
    @PostMapping("/projects")
    public Result<ProjectTreeNodeResponse> createProject(@Valid @RequestBody ProjectRequest request) {
        log.info("管理员创建工点信息，节点类型: {}, 节点名称: {}", request.getNodeType(), request.getProjectName());
        ProjectTreeNodeResponse node = projectService.createProject(request);
        return Result.success(node);
    }
    
    @Operation(summary = "修改工点信息", description = "管理员修改工点信息。可以修改节点名称、编号、描述、状态等，但不能修改节点类型和父节点。", tags = {"管理员管理-工点管理"})
    @PutMapping("/projects/{projectId}")
    public Result<ProjectTreeNodeResponse> updateProject(
            @Parameter(description = "项目节点ID", required = true, example = "1") @PathVariable Long projectId,
            @Valid @RequestBody ProjectRequest request) {
        log.info("管理员修改工点信息，项目ID: {}", projectId);
        ProjectTreeNodeResponse node = projectService.updateProject(projectId, request);
        return Result.success(node);
    }
    
    @Operation(summary = "获取工点进度详情", description = "获取指定工点指定循环的进度详情，包括循环信息、控制总时间、上循环结束时间、本循环开始时间、当前工序和工序列表等详细信息。如果不指定循环号，则返回最新循环的进度详情。", tags = {"管理员管理-工点管理"})
    @GetMapping("/projects/{projectId}/progress")
    public Result<ProgressDetailResponse> getProgressDetail(
            @Parameter(description = "工点项目ID", required = true, example = "1") @PathVariable Long projectId,
            @Parameter(description = "循环号，不指定则返回最新循环", example = "2") @RequestParam(required = false) Integer cycleNumber) {
        log.info("查询工点进度详情，项目ID: {}, 循环号: {}", projectId, cycleNumber);
        ProgressDetailResponse response = projectService.getProgressDetail(projectId, cycleNumber);
        return Result.success(response);
    }
    
    @Operation(summary = "查看工点施工状态", description = "管理员查看各工点的当前施工状态，包括当前工序持续时长、上几道工序的完成情况和节超情况。返回当前正在进行的工序信息和已完成的工序列表。", tags = {"管理员管理-工点管理"})
    @GetMapping("/projects/{projectId}/construction-status")
    public Result<SiteConstructionStatusResponse> getSiteConstructionStatus(
            @Parameter(description = "工点项目ID", required = true, example = "1") @PathVariable Long projectId) {
        log.info("管理员查看工点施工状态，项目ID: {}", projectId);
        SiteConstructionStatusResponse response = projectService.getSiteConstructionStatus(projectId);
        return Result.success(response);
    }
    
    @Operation(summary = "新建循环", description = "为指定工点创建新循环。预估开始时间会自动设置为与实际开始时间一致，预计结束时间会根据实际开始时间和控制时长自动计算。传入工序模板ID（该模板下任意一个工序模板的ID即可），后端会根据模板名称自动创建该模板下的所有工序。", tags = {"管理员管理-循环管理"})
    @PostMapping("/cycles")
    public Result<CycleResponse> createCycle(@Valid @RequestBody CreateCycleRequest request) {
        log.info("创建新循环，项目ID: {}, 模板ID: {}, 控制时长: {}分钟", 
                request.getProjectId(), request.getTemplateId(), request.getControlDuration());
        CycleResponse response = cycleService.createCycle(request);
        return Result.success(response);
    }
    
    @Operation(summary = "获取工点循环列表", description = "分页查询指定工点下的所有循环记录，返回循环的基本信息，包括循环号、状态、时间、进尺、围岩等级等。", tags = {"管理员管理-循环管理"})
    @GetMapping("/projects/{projectId}/cycles")
    public Result<Page<CycleResponse>> getProjectCycles(
            @Parameter(description = "工点项目ID", required = true, example = "1") @PathVariable Long projectId,
            @Parameter(description = "页码，从1开始", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页记录数", example = "10") @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<CycleResponse> page = cycleService.getCyclesByProject(projectId, pageNum, pageSize);
        return Result.success(page);
    }
    
    @Operation(summary = "获取循环详情", description = "查询单个循环的详细信息，包括循环号、控制时长、开始结束时间、进尺、围岩等级等。", tags = {"管理员管理-循环管理"})
    @GetMapping("/cycles/{cycleId}")
    public Result<CycleResponse> getCycleDetail(
            @Parameter(description = "循环ID", required = true, example = "1") @PathVariable Long cycleId) {
        CycleResponse response = cycleService.getCycleDetail(cycleId);
        return Result.success(response);
    }
    
    @Operation(summary = "导出循环报表", description = "基于Excel模板导出循环报表，生成并下载文件。", tags = {"管理员管理-循环管理"})
    @GetMapping("/cycles/{cycleId}/report")
    public void exportCycleReport(
            @Parameter(description = "循环ID", required = true, example = "1") @PathVariable Long cycleId,
            HttpServletResponse response) {
        log.info("导出循环报表，循环ID: {}", cycleId);
        cycleService.exportCycleReport(cycleId, response);
    }
    
    @Operation(summary = "获取循环报表数据", description = "获取循环报表中需要填写的单元格值，返回JSON格式数据，用于前端展示或手动填写Excel。", tags = {"管理员管理-循环管理"})
    @GetMapping("/cycles/{cycleId}/report-data")
    public Result<com.zzw.zzwgx.dto.response.CycleReportDataResponse> getCycleReportData(
            @Parameter(description = "循环ID", required = true, example = "1") @PathVariable Long cycleId) {
        log.info("获取循环报表数据，循环ID: {}", cycleId);
        com.zzw.zzwgx.dto.response.CycleReportDataResponse response = cycleService.getCycleReportData(cycleId);
        return Result.success(response);
    }
    
    @Operation(summary = "更新循环信息", description = "修改循环的控制时长、开始结束时间、状态、进尺、围岩等级等信息。如果更新为进行中状态，会检查该工点是否已有其他进行中的循环。", tags = {"管理员管理-循环管理"})
    @PutMapping("/cycles/{cycleId}")
    public Result<CycleResponse> updateCycle(
            @Parameter(description = "循环ID", required = true, example = "1") @PathVariable Long cycleId,
            @Valid @RequestBody UpdateCycleRequest request) {
        CycleResponse response = cycleService.updateCycle(cycleId, request);
        return Result.success(response);
    }
    
    @Operation(summary = "新建工序", description = "为指定循环创建新工序。需要指定工序字典ID（从工序字典表中选择工序），工序名称会自动从工序字典中获取。控制时长需要用户输入。", tags = {"管理员管理-工序管理"})
    @PostMapping("/processes")
    public Result<ProcessResponse> createProcess(@Valid @RequestBody CreateProcessRequest request) {
        log.info("创建新工序，循环ID: {}, 工序字典ID: {}, 施工人员ID: {}, 控制时长: {}", 
                request.getCycleId(), request.getProcessCatalogId(), request.getWorkerId(), request.getControlTime());
        ProcessResponse response = processService.createProcess(request);
        return Result.success(response);
    }
    
    @Operation(summary = "获取工序详情", description = "获取指定工序的详细信息，包括工序名称、操作员、状态、控制时间、实际时间、超时/节时情况等。", tags = {"管理员管理-工序管理"})
    @GetMapping("/processes/{processId}")
    public Result<ProcessDetailResponse> getProcessDetail(
            @Parameter(description = "工序ID", required = true, example = "1") @PathVariable Long processId) {
        log.info("查询工序详情，工序ID: {}", processId);
        ProcessDetailResponse response = processService.getProcessDetail(processId);
        return Result.success(response);
    }

    @Operation(summary = "更新工序", description = "更新指定工序的信息，包括工序名称、控制时间、状态、开始结束时间、操作员、进尺等字段，支持部分字段更新。注意：各工序控制时间可调整，仅限管理员操作。", tags = {"管理员管理-工序管理"})
    @PutMapping("/processes/{processId}")
    public Result<ProcessResponse> updateProcess(
            @Parameter(description = "工序ID", required = true, example = "1") @PathVariable Long processId,
            @Valid @RequestBody UpdateProcessRequest request) {
        log.info("更新工序，工序ID: {}, 控制时间: {}", processId, request.getControlTime());
        ProcessResponse response = processService.updateProcess(processId, request);
        return Result.success(response);
    }
    
    @Operation(summary = "批量更新工序顺序", description = "批量更新指定循环下工序的执行顺序，支持工序顺序的灵活调整。", tags = {"管理员管理-工序管理"})
    @PutMapping("/cycles/{cycleId}/processes/orders")
    public Result<Void> updateProcessOrders(
            @Parameter(description = "循环ID", required = true, example = "1") @PathVariable Long cycleId,
            @Valid @RequestBody UpdateProcessOrderRequest request) {
        log.info("批量更新工序顺序，循环ID: {}", cycleId);
        processService.updateProcessOrders(cycleId, request);
        return Result.success();
    }
    
    @Operation(summary = "计算循环工序总时间", description = "计算指定循环的工序总时间统计。返回单工序总时间（所有工序实际完成时间的总和）和整套工序总时间（考虑重叠时间不重复计算）。单工序时间依旧按照实际完成时间进行统计。", tags = {"管理员管理-工序管理"})
    @GetMapping("/cycles/{cycleId}/process-time")
    public Result<com.zzw.zzwgx.dto.response.CycleProcessTimeResponse> calculateCycleProcessTime(
            @Parameter(description = "循环ID", required = true, example = "1") @PathVariable Long cycleId) {
        log.info("计算循环工序总时间，循环ID: {}", cycleId);
        com.zzw.zzwgx.dto.response.CycleProcessTimeResponse response = processService.calculateCycleProcessTime(cycleId);
        return Result.success(response);
    }
    
    @Operation(summary = "查询超时未填报原因的工序列表", description = "查询所有超时但未填报超时原因的工序列表，仅返回循环未完成的工序。用于管理员督促施工人员填报超时原因。返回信息包括工点名称、工序信息、超时时间等。", tags = {"管理员管理-工序管理"})
    @GetMapping("/processes/overtime-without-reason")
    public Result<Page<OvertimeProcessResponse>> getOvertimeProcessesWithoutReason(
            @Parameter(description = "页码，从1开始", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页记录数", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "工点名称关键词，支持模糊搜索", example = "工点1") @RequestParam(required = false) String projectName) {
        log.info("查询超时未填报原因的工序列表，页码: {}, 大小: {}, 工点名称: {}", pageNum, pageSize, projectName);
        Page<OvertimeProcessResponse> page = processService.getOvertimeProcessesWithoutReason(pageNum, pageSize, projectName);
        return Result.success(page);
    }
    
    @Operation(summary = "获取所有模板名称列表", description = "获取系统中所有工序模板的名称列表（去重）。", tags = {"管理员管理-工序模板管理"})
    @GetMapping("/process-templates/names")
    public Result<java.util.List<String>> getAllTemplateNames() {
        log.info("查询所有模板名称列表");
        java.util.List<String> templateNames = processTemplateService.getAllTemplateNames();
        return Result.success(templateNames);
    }
    
    @Operation(summary = "获取模板列表", description = "获取系统中所有模板列表，用于前端选择模板。每个模板包含模板名称和对应的templateId（该模板下第一个工序模板的ID），前端选择模板后可以使用返回的templateId来创建循环。", tags = {"管理员管理-工序模板管理"})
    @GetMapping("/templates")
    public Result<java.util.List<TemplateListResponse>> getTemplateList() {
        log.info("查询模板列表");
        java.util.List<TemplateListResponse> templateList = processTemplateService.getTemplateList();
        return Result.success(templateList);
    }
    
    @Operation(summary = "获取所有工序模板选项列表", description = "获取系统中所有工序模板的选项列表，用于前端下拉选择工序名称。返回数据包含模板ID、工序名称、模板名称和控制时间等信息。前端选择工序名称后，可以使用返回的templateId来创建工序。", tags = {"管理员管理-工序模板管理"})
    @GetMapping("/process-templates/options")
    public Result<java.util.List<ProcessTemplateOptionResponse>> getAllProcessTemplateOptions() {
        log.info("查询所有工序模板选项列表");
        java.util.List<ProcessTemplateOptionResponse> options = processTemplateService.getAllProcessTemplateOptions();
        return Result.success(options);
    }
    
    @Operation(summary = "根据模板名称获取工序模板列表", description = "根据模板名称查询该模板下的所有工序定义，按默认顺序排序。", tags = {"管理员管理-工序模板管理"})
    @GetMapping("/process-templates")
    public Result<java.util.List<ProcessTemplateResponse>> getProcessTemplates(
            @Parameter(description = "模板名称", required = true, example = "标准模板") @RequestParam String templateName) {
        log.info("查询工序模板列表，模板名称: {}", templateName);
        java.util.List<com.zzw.zzwgx.entity.ProcessTemplate> templates = processTemplateService.getTemplatesByName(templateName);
        java.util.List<ProcessTemplateResponse> responses = templates.stream()
                .map(processTemplateService::convertToResponse)
                .collect(java.util.stream.Collectors.toList());
        return Result.success(responses);
    }
    
    @Operation(summary = "获取工序模板详情", description = "根据模板ID获取单个工序模板的详细信息。", tags = {"管理员管理-工序模板管理"})
    @GetMapping("/process-templates/{templateId}")
    public Result<ProcessTemplateResponse> getProcessTemplateDetail(
            @Parameter(description = "模板ID", required = true, example = "1") @PathVariable Long templateId) {
        log.info("查询工序模板详情，模板ID: {}", templateId);
        com.zzw.zzwgx.entity.ProcessTemplate template = processTemplateService.getById(templateId);
        if (template == null) {
            return Result.fail(com.zzw.zzwgx.common.enums.ResultCode.TEMPLATE_NOT_FOUND);
        }
        ProcessTemplateResponse response = processTemplateService.convertToResponse(template);
        return Result.success(response);
    }
    
    @Operation(summary = "创建工序模板", description = "创建新的工序模板项。", tags = {"管理员管理-工序模板管理"})
    @PostMapping("/process-templates")
    public Result<ProcessTemplateResponse> createProcessTemplate(@Valid @RequestBody CreateProcessTemplateRequest request) {
        log.info("创建工序模板，模板名称: {}, 工序名称: {}", request.getTemplateName(), request.getProcessName());
        com.zzw.zzwgx.entity.ProcessTemplate template = new com.zzw.zzwgx.entity.ProcessTemplate();
        template.setTemplateName(request.getTemplateName());
        template.setProcessName(request.getProcessName());
        template.setControlTime(request.getControlTime());
        template.setDefaultOrder(request.getDefaultOrder());
        template.setDescription(request.getDescription());
        processTemplateService.save(template);
        ProcessTemplateResponse response = processTemplateService.convertToResponse(template);
        return Result.success(response);
    }
    
    @Operation(summary = "更新工序模板", description = "更新指定工序模板的信息，支持部分字段更新。", tags = {"管理员管理-工序模板管理"})
    @PutMapping("/process-templates/{templateId}")
    public Result<ProcessTemplateResponse> updateProcessTemplate(
            @Parameter(description = "模板ID", required = true, example = "1") @PathVariable Long templateId,
            @Valid @RequestBody UpdateProcessTemplateRequest request) {
        log.info("更新工序模板，模板ID: {}", templateId);
        com.zzw.zzwgx.entity.ProcessTemplate template = processTemplateService.getById(templateId);
        if (template == null) {
            return Result.fail(com.zzw.zzwgx.common.enums.ResultCode.TEMPLATE_NOT_FOUND);
        }
        if (request.getTemplateName() != null) {
            template.setTemplateName(request.getTemplateName());
        }
        if (request.getProcessName() != null) {
            template.setProcessName(request.getProcessName());
        }
        if (request.getControlTime() != null) {
            template.setControlTime(request.getControlTime());
        }
        if (request.getDefaultOrder() != null) {
            template.setDefaultOrder(request.getDefaultOrder());
        }
        if (request.getDescription() != null) {
            template.setDescription(request.getDescription());
        }
        processTemplateService.updateById(template);
        ProcessTemplateResponse response = processTemplateService.convertToResponse(template);
        return Result.success(response);
    }
    
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
    public Result<java.util.List<UserViewListResponse>> listWorkers(
            @Parameter(description = "项目ID（可选，传入则仅返回该项目已有参与记录的施工人员）", example = "7") @RequestParam(required = false) Long projectId,
            @Parameter(description = "用户名或姓名关键词", example = "张") @RequestParam(required = false) String keyword) {
        java.util.List<UserViewListResponse> workers = userService.listWorkers(projectId, keyword);
        return Result.success(workers);
    }
    
    @Operation(summary = "创建用户账号", description = "管理员为填报人员创建账号。每个填报人员固定一个账号，便于区分。可以指定账号、密码、姓名、身份证号、手机号、角色等信息。", tags = {"管理员管理-用户管理"})
    @PostMapping("/users")
    public Result<UserListResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("管理员创建用户账号，用户名: {}, 姓名: {}, 角色: {}", 
                request.getUsername(), request.getRealName(), request.getRoleCode());
        com.zzw.zzwgx.entity.User user = userService.createUser(request);
        
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
    
    @Operation(summary = "修改用户账号", description = "管理员修改用户账号信息。可以修改姓名、身份证号、手机号、密码、状态、角色等信息。", tags = {"管理员管理-用户管理"})
    @PutMapping("/users/{userId}")
    public Result<UserListResponse> updateUser(
            @Parameter(description = "用户ID", required = true, example = "1") @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request) {
        log.info("管理员修改用户账号，用户ID: {}", userId);
        com.zzw.zzwgx.entity.User user = userService.updateUser(userId, request);
        
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
    
    @Operation(summary = "获取工序字典列表", description = "获取所有工序字典列表，按显示顺序排序。用于管理员查看和管理所有可用的工序。", tags = {"管理员管理-工序字典管理"})
    @GetMapping("/process-catalogs")
    public Result<java.util.List<ProcessCatalogResponse>> getProcessCatalogs() {
        log.info("查询工序字典列表");
        java.util.List<ProcessCatalogResponse> catalogs = processCatalogService.getAllProcessCatalogs();
        return Result.success(catalogs);
    }
    
    @Operation(summary = "创建工序字典", description = "创建新的工序字典项。工序名称必须唯一。", tags = {"管理员管理-工序字典管理"})
    @PostMapping("/process-catalogs")
    public Result<ProcessCatalogResponse> createProcessCatalog(@Valid @RequestBody CreateProcessCatalogRequest request) {
        log.info("创建工序字典，工序名称: {}", request.getProcessName());
        ProcessCatalogResponse response = processCatalogService.createProcessCatalog(request);
        return Result.success(response);
    }
    
    @Operation(summary = "更新工序字典", description = "更新工序字典信息，包括工序名称、编码、描述、显示顺序、状态等。", tags = {"管理员管理-工序字典管理"})
    @PutMapping("/process-catalogs/{catalogId}")
    public Result<ProcessCatalogResponse> updateProcessCatalog(
            @Parameter(description = "工序字典ID", required = true, example = "1") @PathVariable Long catalogId,
            @Valid @RequestBody UpdateProcessCatalogRequest request) {
        log.info("更新工序字典，工序字典ID: {}", catalogId);
        ProcessCatalogResponse response = processCatalogService.updateProcessCatalog(catalogId, request);
        return Result.success(response);
    }
    
    @Operation(summary = "批量调整工序顺序", description = "批量调整工序的显示顺序。用于调整工序间的先后顺序。", tags = {"管理员管理-工序字典管理"})
    @PutMapping("/process-catalogs/orders")
    public Result<Void> updateProcessCatalogOrder(@Valid @RequestBody UpdateProcessCatalogOrderRequest request) {
        log.info("批量调整工序顺序，工序数量: {}", request.getOrders().size());
        processCatalogService.updateProcessCatalogOrder(request);
        return Result.success();
    }
}

