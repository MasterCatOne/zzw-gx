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
import com.zzw.zzwgx.entity.Cycle;
import com.zzw.zzwgx.entity.Process;
import com.zzw.zzwgx.entity.Project;
import com.zzw.zzwgx.mapper.ProjectMapper;
import com.zzw.zzwgx.service.CycleService;
import com.zzw.zzwgx.security.SecurityUtils;
import com.zzw.zzwgx.service.ProcessService;
import com.zzw.zzwgx.service.ProjectService;
import com.zzw.zzwgx.service.UserProjectService;
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
    public Page<ProjectListResponse> getProjectList(Integer pageNum, Integer pageSize, String name, String status) {
        log.info("查询工点列表，页码: {}, 每页大小: {}, 名称关键词: {}, 状态: {}", pageNum, pageSize, name, status);

        // 权限控制：系统管理员查看全部，普通管理员按分配的工点过滤
        List<Long> allowedProjectIds = getAllowedProjectIdsForCurrentUser(pageNum, pageSize);
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
            response.setId(project.getId());
            response.setParentId(project.getParentId());
            response.setNodeType(project.getNodeType());
            response.setProjectName(project.getProjectName());
            response.setProjectCode(project.getProjectCode());
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
        if (currentCycle.getControlDuration() != null && currentCycle.getControlDuration() > 0) {
            BigDecimal speed = BigDecimal.ZERO;
            if (currentCycle.getAdvanceLength() != null) {
                speed = currentCycle.getAdvanceLength()
                        .divide(BigDecimal.valueOf(currentCycle.getControlDuration()), 2, java.math.RoundingMode.HALF_UP);
            }
            response.setControlSpeedPerHour(speed);
        }
        
        // 获取当前工序
        List<Process> processes = processService.getProcessesByCycleId(currentCycle.getId());
        Process currentProcess = processes.stream()
                .filter(p -> ProcessStatus.IN_PROGRESS.getCode().equals(p.getProcessStatus()))
                .findFirst()
                .orElse(null);
        if (currentProcess != null) {
            response.setCurrentProcess(currentProcess.getProcessName());
        }
        
        // 上循环结束时间
        if (lastCycle != null) {
            List<Process> lastCycleProcesses = processService.getProcessesByCycleId(lastCycle.getId());
            Process lastProcess = lastCycleProcesses.stream()
                    .filter(p -> ProcessStatus.COMPLETED.getCode().equals(p.getProcessStatus()))
                    .reduce((first, second) -> second)
                    .orElse(null);
            if (lastProcess != null && lastProcess.getActualEndTime() != null) {
                response.setLastCycleEndTime(lastProcess.getActualEndTime());
            }
        }
        
        // 本循环开始时间
        response.setCurrentCycleStartTime(currentCycle.getStartDate());
        
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
            
            // 计算实际时间
            if (process.getActualStartTime() != null && process.getActualEndTime() != null) {
                long minutes = Duration.between(process.getActualStartTime(), process.getActualEndTime()).toMinutes();
                info.setActualTime((int) minutes);
                info.setElapsedMinutes((int) minutes);
                if (process.getEstimatedEndTime() != null) {
                    long diff = Duration.between(process.getEstimatedEndTime(), process.getActualEndTime()).toMinutes();
                    info.setTimeDifferenceMinutes((int) diff);
                }
            } else if (process.getActualStartTime() != null) {
                long minutes = Duration.between(process.getActualStartTime(), LocalDateTime.now()).toMinutes();
                info.setElapsedMinutes((int) minutes);
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
        project.setParentId(request.getParentId());
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
     * 获取当前用户允许查看的工点ID列表
     * SYSTEMADMIN 角色返回 null 表示不过滤；普通管理员返回其分配的工点ID列表；其他角色返回空列表
     */
    private List<Long> getAllowedProjectIdsForCurrentUser(Integer pageNum, Integer pageSize) {
        var currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            return new java.util.ArrayList<>();
        }
        var roles = currentUser.getRoleCodes();
        boolean isSystemAdmin = roles.stream().anyMatch(r -> "SYSTEM_ADMIN".equals(r));
        if (isSystemAdmin) {
            return null;
        }
        Long userId = currentUser.getUserId();
        List<Long> assignedProjectIds = userProjectService.getProjectIdsByUser(userId);
        if (CollectionUtils.isEmpty(assignedProjectIds)) {
            return new java.util.ArrayList<>();
        }
        return expandToSiteProjectIds(assignedProjectIds);
    }

    /**
     * 将任意层级的节点展开为所有子层级的工点（node_type=SITE）ID
     */
    private List<Long> expandToSiteProjectIds(List<Long> rootIds) {
        List<Long> result = new java.util.ArrayList<>();
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
}

