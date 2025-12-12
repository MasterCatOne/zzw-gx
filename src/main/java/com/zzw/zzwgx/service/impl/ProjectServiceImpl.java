package com.zzw.zzwgx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzw.zzwgx.common.enums.ProcessStatus;
import com.zzw.zzwgx.common.enums.ProjectStatus;
import com.zzw.zzwgx.common.enums.ResultCode;
import com.zzw.zzwgx.common.enums.RockLevel;
import com.zzw.zzwgx.common.exception.BusinessException;
import com.zzw.zzwgx.dto.request.ProjectRequest;
import com.zzw.zzwgx.dto.response.ProjectListResponse;
import com.zzw.zzwgx.dto.response.ProgressDetailResponse;
import com.zzw.zzwgx.dto.response.ProjectTreeNodeResponse;
import com.zzw.zzwgx.dto.response.SiteConstructionStatusResponse;
import com.zzw.zzwgx.entity.Cycle;
import com.zzw.zzwgx.entity.Process;
import com.zzw.zzwgx.entity.Project;
import com.zzw.zzwgx.entity.User;
import com.zzw.zzwgx.mapper.ProjectMapper;
import com.zzw.zzwgx.service.CycleService;
import com.zzw.zzwgx.security.SecurityUtils;
import com.zzw.zzwgx.service.ProcessService;
import com.zzw.zzwgx.service.ProjectService;
import com.zzw.zzwgx.service.UserProjectService;
import com.zzw.zzwgx.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

/**
 * 项目服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {
    
    private final CycleService cycleService;
    private final ProcessService processService;
    private final UserProjectService userProjectService;
    private final UserService userService;
    
    @Override
    public Page<Project> getProjectPage(Integer pageNum, Integer pageSize, String name) {
        log.debug("分页查询项目，页码: {}, 每页大小: {}, 搜索关键词: {}", pageNum, pageSize, name);
        Page<Project> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(Project::getProjectName, name);
        }
        wrapper.orderByDesc(Project::getCreateTime);
        Page<Project> result = page(page, wrapper);
        log.debug("分页查询项目完成，共查询到 {} 条记录", result.getTotal());
        return result;
    }
    
    @Override
    public Page<ProjectListResponse> getProjectList(Integer pageNum, Integer pageSize, String name, String status, Long userId) {
        log.info("查询工点列表，页码: {}, 每页大小: {}, 名称关键词: {}, 状态: {}, 指定用户ID: {}", pageNum, pageSize, name, status, userId);

        // 权限控制：
        // 1. 如果显式传入 userId（前端联调/测试时使用），则按该用户的工点权限过滤；
        // 2. 如果未传 userId，则按当前登录用户（从 SecurityContext 中获取）进行权限控制。
        List<Long> allowedProjectIds = getAllowedProjectIds(userId);
        Page<Project> page;
        Page<Project> p = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        
        // 只查询工点（SITE类型）
        wrapper.eq(Project::getNodeType, "SITE");
        
        if (allowedProjectIds != null) {
            if (allowedProjectIds.isEmpty()) {
                Page<ProjectListResponse> emptyPage = new Page<>(pageNum, pageSize, 0);
                emptyPage.setRecords(new ArrayList<>());
                return emptyPage;
            }
            // 普通管理员：只查询分配的工点
            wrapper.in(Project::getId, allowedProjectIds);
        }
        // 系统管理员：allowedProjectIds == null，不添加ID过滤，查询所有工点
        
        if (StringUtils.hasText(name)) {
            wrapper.like(Project::getProjectName, name);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Project::getProjectStatus, status);
        }
        page = page(p, wrapper);

        Page<ProjectListResponse> responsePage = new Page<>(pageNum, pageSize, page.getTotal());

        // 由于查询时已经过滤了node_type='SITE'和status，这里直接使用查询结果
        List<Project> siteProjects = page.getRecords();

        List<ProjectListResponse> list = siteProjects.stream().map(project -> {
            ProjectListResponse response = new ProjectListResponse();
            // 只设置前端需要的字段
            response.setId(project.getId());
            response.setProjectName(project.getProjectName());
            response.setProjectStatus(project.getProjectStatus());
            ProjectStatus ps = ProjectStatus.fromCode(project.getProjectStatus());
            response.setStatusDesc(ps != null ? ps.getDesc() : "");

            // 获取最新循环：不管状态，获取循环号最大的循环
            Cycle latestCycle = cycleService.getLatestCycleByProjectId(project.getId());
            if (latestCycle != null) {
                // 有循环，显示最新循环号
                response.setCurrentCycleNumber(latestCycle.getCycleNumber());
                response.setRockLevel(latestCycle.getRockLevel());
                RockLevel rock = RockLevel.fromCode(latestCycle.getRockLevel());
                response.setRockLevelDesc(rock != null ? rock.getDesc() : latestCycle.getRockLevel());
            } else {
                // 没有任何循环，显示0
                response.setCurrentCycleNumber(0);
                response.setRockLevel("-");
                response.setRockLevelDesc("-");
            }
            return response;
        }).collect(Collectors.toList());

        responsePage.setRecords(list);
        log.info("查询工点列表成功，共查询到 {} 条记录", list.size());
        return responsePage;
    }
    
    @Override
    public ProgressDetailResponse getProgressDetail(Long projectId, Integer cycleNumber) {
        log.info("查询项目进度详情，项目ID: {}, 循环号: {}", projectId, cycleNumber);
        Project project = getById(projectId);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        
        Cycle currentCycle;
        if (cycleNumber != null) {
            currentCycle = cycleService.getCycleByProjectAndNumber(projectId, cycleNumber);
        } else {
            // 未指定循环号时，获取最新循环（不管状态）
            currentCycle = cycleService.getLatestCycleByProjectId(projectId);
        }
        if (currentCycle == null) {
            throw new BusinessException(ResultCode.CYCLE_NOT_FOUND);
        }
        //        上一个循环
        Cycle lastCycle = cycleService.getCycleByProjectAndNumber(projectId, currentCycle.getCycleNumber() - 1);
        
        ProgressDetailResponse response = new ProgressDetailResponse();
        response.setCycleId(currentCycle.getId());
        response.setCycleNumber(currentCycle.getCycleNumber());
        response.setCycleStatus(currentCycle.getStatus());
        response.setControlDuration(currentCycle.getControlDuration());
        response.setAdvanceLength(currentCycle.getAdvanceLength());
        response.setEstimatedStartDate(currentCycle.getEstimatedStartDate());
        response.setEstimatedEndDate(currentCycle.getEstimatedEndDate());
        response.setActualStartDate(currentCycle.getStartDate());
        response.setActualEndDate(currentCycle.getEndDate());
        
        // 获取当前循环的所有工序
        List<Process> processes = processService.getProcessesByCycleId(currentCycle.getId());
        
        // 计算控制总时间：循环的控制时长（controlDuration，单位：分钟）转换为小时
        if (currentCycle.getControlDuration() != null) {
            BigDecimal controlTotalTimeHours = BigDecimal.valueOf(currentCycle.getControlDuration())
                    .divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
            response.setControlTotalTimeHours(controlTotalTimeHours);
        }
        Process currentProcess = processes.stream()
                .filter(p -> ProcessStatus.IN_PROGRESS.getCode().equals(p.getProcessStatus()))
                .findFirst()
                .orElse(null);
        if (currentProcess != null) {
            response.setCurrentProcess(currentProcess.getProcessName());
        }
        
        // 上循环结束时间：取上一循环本身的实际结束时间（endDate）
        LocalDateTime lastCycleEnd = null;
        if (lastCycle != null && lastCycle.getEndDate() != null) {
            lastCycleEnd = lastCycle.getEndDate();
            response.setLastCycleEndTime(lastCycleEnd);
        }
        
        // 计算上循环预计结束时间与实际结束时间的时间差（单位：分钟）
        // diff > 0 表示超时 diff 分钟；diff < 0 表示节省 |diff| 分钟；diff == 0 表示按时完成
        if (lastCycle != null && lastCycle.getEstimatedEndDate() != null && lastCycle.getEndDate() != null) {
            long diffMinutes = Duration.between(lastCycle.getEstimatedEndDate(), lastCycle.getEndDate()).toMinutes();
            response.setLastCycleEndRemainingMinutes(diffMinutes);

            String text;
            if (diffMinutes > 0) {
                text = "超时" + diffMinutes + "分钟";
            } else if (diffMinutes < 0) {
                text = "余时" + Math.abs(diffMinutes) + "分钟";
            } else {
                text = "按时完成";
            }
            response.setLastCycleEndRemainingText(text);
        }
        
        // 本循环开始时间
        LocalDateTime currentCycleStart = currentCycle.getStartDate();
        response.setCurrentCycleStartTime(currentCycleStart);
        
        // 计算本循环已进行时间或超时/余时时间
        if (currentCycleStart != null) {
            // 如果循环已完成，计算超时/余时时间（实际结束时间 vs 预计结束时间）
            if ("COMPLETED".equals(currentCycle.getStatus()) && currentCycle.getEndDate() != null) {
                LocalDateTime currentCycleEnd = currentCycle.getEndDate();
                LocalDateTime estimatedEndDate = currentCycle.getEstimatedEndDate();
                
                if (estimatedEndDate != null) {
                    // 计算实际结束时间与预计结束时间的差值（分钟）
                    long diffMinutes = Duration.between(estimatedEndDate, currentCycleEnd).toMinutes();
                    long diffHours = Math.abs(diffMinutes) / 60;
                    long diffRemainingMinutes = Math.abs(diffMinutes) % 60;
                    
                    if (diffMinutes > 0) {
                        // 超时：实际结束时间晚于预计结束时间
                        response.setCurrentCycleElapsedHours(diffHours);
                        if (diffRemainingMinutes > 0) {
                            response.setCurrentCycleElapsedText("超时" + diffHours + "小时" + diffRemainingMinutes + "分钟");
                        } else {
                            response.setCurrentCycleElapsedText("超时" + diffHours + "小时");
                        }
                    } else if (diffMinutes < 0) {
                        // 余时：实际结束时间早于预计结束时间
                        response.setCurrentCycleElapsedHours(diffHours);
                        if (diffRemainingMinutes > 0) {
                            response.setCurrentCycleElapsedText("余时" + diffHours + "小时" + diffRemainingMinutes + "分钟");
                        } else {
                            response.setCurrentCycleElapsedText("余时" + diffHours + "小时");
                        }
                    } else {
                        // 按时完成
                        response.setCurrentCycleElapsedHours(0L);
                        response.setCurrentCycleElapsedText("按时完成");
                    }
                } else {
                    // 没有预计结束时间，显示总耗时
                    long actualTotalMinutes = Duration.between(currentCycleStart, currentCycleEnd).toMinutes();
                    long totalHours = actualTotalMinutes / 60;
                    long totalRemainingMinutes = actualTotalMinutes % 60;
                    response.setCurrentCycleElapsedHours(totalHours);
                    if (totalRemainingMinutes > 0) {
                        response.setCurrentCycleElapsedText("总耗时" + totalHours + "小时" + totalRemainingMinutes + "分钟");
                    } else {
                        response.setCurrentCycleElapsedText("总耗时" + totalHours + "小时");
                    }
                }
            } else {
                // 循环未完成，显示已进行时间
                LocalDateTime now = LocalDateTime.now();
                long elapsedHours = Duration.between(currentCycleStart, now).toHours();
                response.setCurrentCycleElapsedHours(elapsedHours);
                response.setCurrentCycleElapsedText("已进行" + elapsedHours + "小时");
            }
        }
        
        // 工序列表
        List<ProgressDetailResponse.ProcessInfo> processInfos = processes.stream().map(process -> {
            ProgressDetailResponse.ProcessInfo info = new ProgressDetailResponse.ProcessInfo();
            info.setId(process.getId());
            info.setName(process.getProcessName());
            info.setControlTime(process.getControlTime());
            info.setStatus(process.getProcessStatus());
            ProcessStatus status = ProcessStatus.fromCode(process.getProcessStatus());
            info.setStatusDesc(status != null ? status.getDesc() : "");
            info.setActualStartTime(process.getActualStartTime());
            info.setActualEndTime(process.getActualEndTime());
            info.setEstimatedStartTime(process.getEstimatedStartTime());
            info.setEstimatedEndTime(process.getEstimatedEndTime());
            
            // 计算实际时间和节时/超时时间
            if (process.getActualStartTime() != null && process.getActualEndTime() != null) {
                // 工序已完成：计算实际时间与控制时间的差值，显示超时或节时
                long actualMinutes = Duration.between(process.getActualStartTime(), process.getActualEndTime()).toMinutes();
                info.setActualTime((int) actualMinutes);
                info.setElapsedMinutes((int) actualMinutes);
                
                // 计算与控制时间的差值
                if (process.getControlTime() != null) {
                    long diffMinutes = actualMinutes - process.getControlTime();
                    info.setTimeDifferenceMinutes((int) diffMinutes);
                    
                    // 生成文本描述
                    if (diffMinutes > 0) {
                        info.setTimeDifferenceText("超时" + diffMinutes + "分钟");
                    } else if (diffMinutes < 0) {
                        info.setTimeDifferenceText("节时" + Math.abs(diffMinutes) + "分钟");
                    } else {
                        info.setTimeDifferenceText("按时完成");
                    }
                } else {
                    // 没有控制时间，使用预计结束时间计算
                    if (process.getEstimatedEndTime() != null) {
                        long diff = Duration.between(process.getEstimatedEndTime(), process.getActualEndTime()).toMinutes();
                        info.setTimeDifferenceMinutes((int) diff);
                        if (diff > 0) {
                            info.setTimeDifferenceText("超时" + diff + "分钟");
                        } else if (diff < 0) {
                            info.setTimeDifferenceText("节时" + Math.abs(diff) + "分钟");
                        } else {
                            info.setTimeDifferenceText("按时完成");
                        }
                    }
                }
            } else if (process.getActualStartTime() != null) {
                // 工序正在进行中：显示已进行时长（>=60分钟转小时+分钟）
                long elapsedMinutes = Duration.between(process.getActualStartTime(), LocalDateTime.now()).toMinutes();
                info.setElapsedMinutes((int) elapsedMinutes);
                if (elapsedMinutes >= 60) {
                    long hoursPart = elapsedMinutes / 60;
                    long minutesPart = elapsedMinutes % 60;
                    info.setTimeDifferenceText(minutesPart > 0
                            ? "已进行" + hoursPart + "小时" + minutesPart + "分钟"
                            : "已进行" + hoursPart + "小时");
                } else {
                    info.setTimeDifferenceText("已进行" + elapsedMinutes + "分钟");
                }
            }
            if (ProcessStatus.COMPLETED.getCode().equals(process.getProcessStatus())) {
                info.setEndProcess(true);
            }
            
            return info;
        }).collect(Collectors.toList());
        
        response.setProcesses(processInfos);
        log.info("查询项目进度详情成功，项目ID: {}, 循环ID: {}, 工序数量: {}", projectId, currentCycle.getId(), processInfos.size());
        return response;
    }
    
    @Override
    public List<ProjectTreeNodeResponse> getProjectTree() {
        log.info("查询项目树结构");
        List<Project> projects = list(new LambdaQueryWrapper<Project>()
                .eq(Project::getDeleted, 0)
                .orderByAsc(Project::getCreateTime));
        Map<Long, ProjectTreeNodeResponse> nodeMap = new LinkedHashMap<>();
        List<ProjectTreeNodeResponse> roots = new ArrayList<>();
        
        for (Project project : projects) {
            nodeMap.put(project.getId(), convertToNode(project));
        }
        
        for (Project project : projects) {
            ProjectTreeNodeResponse node = nodeMap.get(project.getId());
            if (project.getParentId() == null) {
                roots.add(node);
                continue;
            }
            ProjectTreeNodeResponse parent = nodeMap.get(project.getParentId());
            if (parent != null) {
                parent.getChildren().add(node);
            } else {
                roots.add(node);
            }
        }
        
        log.info("项目树结构构建完成，共有根节点 {} 个", roots.size());
        return roots;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectTreeNodeResponse createProject(ProjectRequest request) {
        log.info("创建项目节点，名称: {}, 类型: {}", request.getProjectName(), request.getNodeType());
        validateParent(request.getParentId(), null);
        validateProjectCodeUnique(request.getProjectCode(), null);
        
        Project project = new Project();
        // 业务约定：当前只新增隧道和工点
        // - 隧道：如果前端未传父节点，默认挂在 ID=149 的项目/父节点下
        // - 工点：必须由前端传入隧道的 ID 作为 parentId
        if ("TUNNEL".equalsIgnoreCase(request.getNodeType()) && request.getParentId() == null) {
            project.setParentId(149L);
        }else{
            project.setParentId(request.getParentId());
        }
        project.setNodeType(request.getNodeType());
        project.setProjectName(request.getProjectName());
        project.setProjectCode(request.getProjectCode());
        project.setProjectDescription(request.getProjectDescription());
        project.setProjectStatus(request.getProjectStatus());
        save(project);
        log.info("项目节点创建成功，ID: {}", project.getId());
        return convertToNode(project);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectTreeNodeResponse updateProject(Long projectId, ProjectRequest request) {
        log.info("更新项目节点，ID: {}", projectId);
        Project project = getById(projectId);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        if (request.getParentId() != null && request.getParentId().equals(projectId)) {
            throw new BusinessException("父节点不能指向自身");
        }
        validateParent(request.getParentId(), projectId);
        validateProjectCodeUnique(request.getProjectCode(), projectId);
        
        project.setParentId(request.getParentId());
        project.setNodeType(request.getNodeType());
        project.setProjectName(request.getProjectName());
        project.setProjectCode(request.getProjectCode());
        project.setProjectDescription(request.getProjectDescription());
        project.setProjectStatus(request.getProjectStatus());
        updateById(project);
        log.info("项目节点更新成功，ID: {}", projectId);
        return convertToNode(project);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProject(Long projectId) {
        log.info("删除项目节点，ID: {}", projectId);
        Project project = getById(projectId);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        long childCount = count(new LambdaQueryWrapper<Project>()
                .eq(Project::getParentId, projectId));
        if (childCount > 0) {
            throw new BusinessException("请先删除子节点");
        }
        removeById(projectId);
        log.info("项目节点删除成功，ID: {}", projectId);
    }
    
    @Override
    public List<com.zzw.zzwgx.dto.response.TunnelOptionResponse> listTunnels() {
        log.info("查询隧道列表（仅ID和名称）");
        List<Project> tunnels = list(new LambdaQueryWrapper<Project>()
                .eq(Project::getNodeType, "TUNNEL")
                .eq(Project::getDeleted, 0)
                .orderByAsc(Project::getId));
        return tunnels.stream().map(p -> {
            com.zzw.zzwgx.dto.response.TunnelOptionResponse resp = new com.zzw.zzwgx.dto.response.TunnelOptionResponse();
            resp.setId(p.getId());
            resp.setName(p.getProjectName());
            return resp;
        }).collect(Collectors.toList());
    }
    
    @Override
    public List<com.zzw.zzwgx.dto.response.SiteOptionResponse> listSitesByTunnelId(Long tunnelId) {
        log.info("根据隧道ID查询工点列表，隧道ID: {}", tunnelId);
        // 验证隧道是否存在
        Project tunnel = getById(tunnelId);
        if (tunnel == null) {
            log.warn("隧道不存在，隧道ID: {}", tunnelId);
            return new ArrayList<>();
        }
        if (!"TUNNEL".equals(tunnel.getNodeType())) {
            log.warn("节点不是隧道类型，节点ID: {}, 节点类型: {}", tunnelId, tunnel.getNodeType());
            return new ArrayList<>();
        }
        
        // 查询该隧道下的所有工点（SITE类型）
        List<Project> sites = list(new LambdaQueryWrapper<Project>()
                .eq(Project::getParentId, tunnelId)
                .eq(Project::getNodeType, "SITE")
                .eq(Project::getDeleted, 0)
                .orderByAsc(Project::getId));
        
        return sites.stream().map(site -> {
            com.zzw.zzwgx.dto.response.SiteOptionResponse resp = new com.zzw.zzwgx.dto.response.SiteOptionResponse();
            resp.setId(site.getId());
            resp.setName(site.getProjectName());
            return resp;
        }).collect(Collectors.toList());
    }
    
    private void validateParent(Long parentId, Long currentId) {
        if (parentId == null) {
            return;
        }
        Project parent = getById(parentId);
        if (parent == null || (currentId != null && parentId.equals(currentId))) {
            throw new BusinessException("父节点不存在");
        }
    }
    
    private void validateProjectCodeUnique(String projectCode, Long currentId) {
        if (!StringUtils.hasText(projectCode)) {
            return;
        }
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Project::getProjectCode, projectCode);
        if (currentId != null) {
            wrapper.ne(Project::getId, currentId);
        }
        long count = count(wrapper);
        if (count > 0) {
            throw new BusinessException("节点编码已存在");
        }
    }
    
    private ProjectTreeNodeResponse convertToNode(Project project) {
        ProjectTreeNodeResponse node = new ProjectTreeNodeResponse();
        node.setId(project.getId());
        node.setParentId(project.getParentId());
        node.setNodeType(project.getNodeType());
        node.setProjectName(project.getProjectName());
        node.setProjectCode(project.getProjectCode());
        node.setProjectDescription(project.getProjectDescription());
        node.setProjectStatus(project.getProjectStatus());
        ProjectStatus status = ProjectStatus.fromCode(project.getProjectStatus());
        node.setStatusDesc(status != null ? status.getDesc() : "");
        return node;
    }

    /**
     * 获取允许查看的工点ID列表
     *
     * @param explicitUserId 显式传入的用户ID（用于前端联调/测试）。如果为 null，则使用当前登录用户。
     *                       - 显式 userId 不做角色区分，直接按该用户被分配的工点过滤。
     *                       - 未传 userId 时：SYSTEM_ADMIN 角色返回 null 表示不过滤；普通管理员返回其分配的工点ID列表；其他角色返回空列表。
     */
    private List<Long> getAllowedProjectIds(Long explicitUserId) {
        // 场景1：显式传入 userId（当前前端联调阶段推荐用法）
        if (explicitUserId != null) {
            List<Long> assignedProjectIds = userProjectService.getProjectIdsByUser(explicitUserId);
            if (CollectionUtils.isEmpty(assignedProjectIds)) {
                return new ArrayList<>();
            }
            return expandToSiteProjectIds(assignedProjectIds);
        }

        // 场景2：未传 userId，走原来的基于当前登录用户的权限控制逻辑
        var currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            return new ArrayList<>();
        }
        var roles = currentUser.getRoleCodes();
        boolean isSystemAdmin = roles.stream().anyMatch(r -> "SYSTEM_ADMIN".equals(r));
        if (isSystemAdmin) {
            // 系统管理员查看所有工点，不做ID过滤
            return null;
        }
        Long userId = currentUser.getUserId();
        List<Long> assignedProjectIds = userProjectService.getProjectIdsByUser(userId);
        if (CollectionUtils.isEmpty(assignedProjectIds)) {
            return new ArrayList<>();
        }
        return expandToSiteProjectIds(assignedProjectIds);
    }

    /**
     * 将任意层级的节点展开为所有子层级的工点（node_type=SITE）ID
     */
    private List<Long> expandToSiteProjectIds(List<Long> rootIds) {
        List<Long> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(rootIds)) {
            return result;
        }
        Queue<Long> queue = new ArrayDeque<>(rootIds);
        while (!queue.isEmpty()) {
            Long currentId = queue.poll();
            Project project = getById(currentId);
            if (project == null) {
                continue;
            }
            if ("SITE".equalsIgnoreCase(project.getNodeType())) {
                result.add(project.getId());
            } else {
                List<Project> children = list(new LambdaQueryWrapper<Project>().eq(Project::getParentId, project.getId()));
                if (!children.isEmpty()) {
                    children.forEach(child -> queue.offer(child.getId()));
                }
            }
        }
        return result;
    }
    
    @Override
    public SiteConstructionStatusResponse getSiteConstructionStatus(Long projectId) {
        log.info("查询工点施工状态，项目ID: {}", projectId);
        
        // 获取工点信息
        Project project = getById(projectId);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        
        // 验证是否为工点（SITE类型）
        if (!"SITE".equalsIgnoreCase(project.getNodeType())) {
            log.warn("查询施工状态失败，节点不是工点类型，项目ID: {}, 节点类型: {}", projectId, project.getNodeType());
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        
        SiteConstructionStatusResponse response = new SiteConstructionStatusResponse();
        response.setProjectId(project.getId());
        response.setProjectName(project.getProjectName());
        response.setProjectStatus(project.getProjectStatus());
        
        // 获取当前循环（最新循环）
        Cycle currentCycle = cycleService.getLatestCycleByProjectId(projectId);
        if (currentCycle == null) {
            log.info("工点暂无循环数据，项目ID: {}", projectId);
            return response;
        }
        
        // 设置循环信息
        response.setCycleId(currentCycle.getId());
        response.setCycleNumber(currentCycle.getCycleNumber());
        response.setCycleStatus(currentCycle.getStatus());
        
        // 设置循环状态描述
        String cycleStatusDesc = "进行中";
        if ("COMPLETED".equals(currentCycle.getStatus())) {
            cycleStatusDesc = "已完成";
        } else if ("IN_PROGRESS".equals(currentCycle.getStatus())) {
            cycleStatusDesc = "进行中";
        }
        response.setCycleStatusDesc(cycleStatusDesc);
        
        // 计算循环已进行时长
        LocalDateTime cycleStartTime = currentCycle.getStartDate();
        response.setCycleStartTime(cycleStartTime);
        if (cycleStartTime != null) {
            LocalDateTime now = LocalDateTime.now();
            long elapsedMinutes = Duration.between(cycleStartTime, now).toMinutes();
            response.setCycleElapsedMinutes(elapsedMinutes);
            
            long hours = elapsedMinutes / 60;
            long minutes = elapsedMinutes % 60;
            if (hours > 0) {
                response.setCycleElapsedText(String.format("已进行%d小时%d分钟", hours, minutes));
            } else {
                response.setCycleElapsedText(String.format("已进行%d分钟", minutes));
            }
        }
        
        // 获取当前循环的所有工序
        List<Process> processes = processService.getProcessesByCycleId(currentCycle.getId());
        if (CollectionUtils.isEmpty(processes)) {
            log.info("当前循环暂无工序数据，循环ID: {}", currentCycle.getId());
            return response;
        }
        
        // 按开始顺序排序
        processes.sort((p1, p2) -> {
            int order1 = p1.getStartOrder() != null ? p1.getStartOrder() : 0;
            int order2 = p2.getStartOrder() != null ? p2.getStartOrder() : 0;
            return Integer.compare(order1, order2);
        });
        
        // 查找当前正在进行的工序
        Process currentProcess = processes.stream()
                .filter(p -> ProcessStatus.IN_PROGRESS.getCode().equals(p.getProcessStatus()))
                .findFirst()
                .orElse(null);
        
        if (currentProcess != null) {
            SiteConstructionStatusResponse.CurrentProcessInfo currentInfo = 
                    new SiteConstructionStatusResponse.CurrentProcessInfo();
            currentInfo.setProcessId(currentProcess.getId());
            currentInfo.setProcessName(currentProcess.getProcessName());
            currentInfo.setStartTime(currentProcess.getActualStartTime());
            currentInfo.setControlTime(currentProcess.getControlTime());
            
            // 计算持续时长
            if (currentProcess.getActualStartTime() != null) {
                LocalDateTime now = LocalDateTime.now();
                long durationMinutes = Duration.between(currentProcess.getActualStartTime(), now).toMinutes();
                currentInfo.setDurationMinutes(durationMinutes);
                
                long hours = durationMinutes / 60;
                long minutes = durationMinutes % 60;
                if (hours > 0) {
                    currentInfo.setDurationText(String.format("已进行%d小时%d分钟", hours, minutes));
                } else {
                    currentInfo.setDurationText(String.format("已进行%d分钟", minutes));
                }
                
                // 判断是否超时
                if (currentProcess.getControlTime() != null) {
                    boolean isOvertime = durationMinutes > currentProcess.getControlTime();
                    currentInfo.setIsOvertime(isOvertime);
                    
                    long remainingMinutes = currentProcess.getControlTime() - durationMinutes;
                    if (remainingMinutes > 0) {
                        long remainingHours = remainingMinutes / 60;
                        long remainingMins = remainingMinutes % 60;
                        if (remainingHours > 0) {
                            currentInfo.setTimeStatusText(String.format("已进行%d小时%d分钟，剩余%d小时%d分钟", 
                                    hours, minutes, remainingHours, remainingMins));
                        } else {
                            currentInfo.setTimeStatusText(String.format("已进行%d分钟，剩余%d分钟", 
                                    minutes, remainingMins));
                        }
                    } else {
                        long overtimeMinutes = -remainingMinutes;
                        long overtimeHours = overtimeMinutes / 60;
                        long overtimeMins = overtimeMinutes % 60;
                        if (overtimeHours > 0) {
                            currentInfo.setTimeStatusText(String.format("已进行%d小时%d分钟，超时%d小时%d分钟", 
                                    hours, minutes, overtimeHours, overtimeMins));
                        } else {
                            currentInfo.setTimeStatusText(String.format("已进行%d分钟，超时%d分钟", 
                                    minutes, overtimeMins));
                        }
                    }
                }
            }
            
            // 获取操作员姓名
            if (currentProcess.getOperatorId() != null) {
                User operator = userService.getById(currentProcess.getOperatorId());
                if (operator != null) {
                    currentInfo.setOperatorName(operator.getRealName());
                }
            }
            
            response.setCurrentProcess(currentInfo);
        }
        
        // 获取已完成的工序列表（上几道工序）
        List<Process> completedProcesses = processes.stream()
                .filter(p -> ProcessStatus.COMPLETED.getCode().equals(p.getProcessStatus()))
                .collect(Collectors.toList());
        
        List<SiteConstructionStatusResponse.CompletedProcessInfo> completedInfos = 
                completedProcesses.stream().map(process -> {
                    SiteConstructionStatusResponse.CompletedProcessInfo info = 
                            new SiteConstructionStatusResponse.CompletedProcessInfo();
                    info.setProcessId(process.getId());
                    info.setProcessName(process.getProcessName());
                    info.setStartTime(process.getActualStartTime());
                    info.setEndTime(process.getActualEndTime());
                    info.setControlTime(process.getControlTime());
                    info.setOvertimeReason(process.getOvertimeReason());
                    
                    // 计算实际耗时和节超情况
                    if (process.getActualStartTime() != null && process.getActualEndTime() != null) {
                        long actualMinutes = Duration.between(
                                process.getActualStartTime(), 
                                process.getActualEndTime()).toMinutes();
                        info.setActualTime((int) actualMinutes);
                        
                        if (process.getControlTime() != null) {
                            int timeDifference = (int) (actualMinutes - process.getControlTime());
                            info.setTimeDifference(timeDifference);
                            info.setIsOvertime(timeDifference > 0);
                            
                            if (timeDifference > 0) {
                                long overtimeHours = timeDifference / 60;
                                long overtimeMins = timeDifference % 60;
                                if (overtimeHours > 0) {
                                    info.setTimeStatusText(String.format("超时%d小时%d分钟", 
                                            overtimeHours, overtimeMins));
                                } else {
                                    info.setTimeStatusText(String.format("超时%d分钟", overtimeMins));
                                }
                            } else if (timeDifference < 0) {
                                long savedHours = (-timeDifference) / 60;
                                long savedMins = (-timeDifference) % 60;
                                if (savedHours > 0) {
                                    info.setTimeStatusText(String.format("节省%d小时%d分钟", 
                                            savedHours, savedMins));
                                } else {
                                    info.setTimeStatusText(String.format("节省%d分钟", savedMins));
                                }
                            } else {
                                info.setTimeStatusText("按时完成");
                            }
                        }
                    }
                    
                    // 获取操作员姓名
                    if (process.getOperatorId() != null) {
                        User operator = userService.getById(process.getOperatorId());
                        if (operator != null) {
                            info.setOperatorName(operator.getRealName());
                        }
                    }
                    
                    return info;
                }).collect(Collectors.toList());
        
        response.setCompletedProcesses(completedInfos);
        
        log.info("查询工点施工状态成功，项目ID: {}, 当前工序: {}, 已完成工序数: {}", 
                projectId, 
                currentProcess != null ? currentProcess.getProcessName() : "无",
                completedInfos.size());
        
        return response;
    }
}

