package com.zzw.zzwgx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzw.zzwgx.common.enums.ResultCode;
import com.zzw.zzwgx.common.exception.BusinessException;
import com.zzw.zzwgx.entity.Project;
import com.zzw.zzwgx.entity.User;
import com.zzw.zzwgx.entity.UserProject;
import com.zzw.zzwgx.mapper.ProjectMapper;
import com.zzw.zzwgx.mapper.UserMapper;
import com.zzw.zzwgx.mapper.UserProjectMapper;
import com.zzw.zzwgx.service.UserProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户项目关联服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProjectServiceImpl extends ServiceImpl<UserProjectMapper, UserProject> implements UserProjectService {
    
    private final UserMapper userMapper;
    private final ProjectMapper projectMapper;
    
    @Override
    public List<Long> getProjectIdsByUser(Long userId) {
        log.info("查询用户管理的项目，用户ID: {}", userId);
        validateUserExist(userId);
        List<UserProject> relations = list(new LambdaQueryWrapper<UserProject>()
                .eq(UserProject::getUserId, userId));
        if (CollectionUtils.isEmpty(relations)) {
            return Collections.emptyList();
        }
        return relations.stream()
                .map(UserProject::getProjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void assignProjects(Long userId, List<Long> projectIds) {
        log.info("分配用户项目，用户ID: {}, 项目IDs: {}", userId, projectIds);
        validateUserExist(userId);
        List<Long> normalizedIds = normalizeProjectIds(projectIds);
        validateProjects(normalizedIds);
        // 先清理该用户已有的重复关联（只保留每个projectId的最小id一条，其余标记删除）
        dedupUserProjects(userId);
        
        // 如果未传任何项目，则仅将现有关联标记为删除
        if (CollectionUtils.isEmpty(normalizedIds)) {
            update(new LambdaUpdateWrapper<UserProject>()
                    .eq(UserProject::getUserId, userId)
                    .set(UserProject::getDeleted, 1));
            log.info("已清空用户项目关联，用户ID: {}", userId);
            return;
        }

        // 1) 将不在目标列表内的关联标记为删除
        update(new LambdaUpdateWrapper<UserProject>()
                .eq(UserProject::getUserId, userId)
                .notIn(UserProject::getProjectId, normalizedIds)
                .set(UserProject::getDeleted, 1));

        // 2) 对于目标列表逐项执行 upsert（ON DUPLICATE KEY UPDATE deleted=0）
        int upserted = 0;
        for (Long projectId : normalizedIds) {
            baseMapper.upsertUserProject(userId, projectId);
            upserted++;
        }

        log.info("用户项目分配完成，用户ID: {}, 目标数量: {}, upsert数量: {}", userId, normalizedIds.size(), upserted);
    }

    /**
     * 去重：同一用户同一项目如有多条记录，仅保留id最小的一条，其余标记为删除
     */
    private void dedupUserProjects(Long userId) {
        List<UserProject> relations = list(new LambdaQueryWrapper<UserProject>()
                .eq(UserProject::getUserId, userId));
        if (CollectionUtils.isEmpty(relations)) {
            return;
        }

        Map<Long, List<UserProject>> grouped = relations.stream()
                .filter(up -> up.getProjectId() != null)
                .collect(Collectors.groupingBy(UserProject::getProjectId));

        List<UserProject> toUpdate = grouped.values().stream()
                .filter(list -> list.size() > 1)
                .flatMap(list -> {
                    // 按id升序，保留第一条，其余标记删除
                    List<UserProject> sorted = list.stream()
                            .sorted(Comparator.comparing(UserProject::getId, Comparator.nullsLast(Long::compareTo)))
                            .toList();
                    return sorted.stream().skip(1).peek(up -> up.setDeleted(1));
                })
                .collect(Collectors.toList());

        if (!toUpdate.isEmpty()) {
            updateBatchById(toUpdate);
            log.info("去重用户项目关联，用户ID: {}, 处理条数: {}", userId, toUpdate.size());
        }
    }
    
    private void validateUserExist(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
    }
    
    private List<Long> normalizeProjectIds(List<Long> projectIds) {
        if (CollectionUtils.isEmpty(projectIds)) {
            return Collections.emptyList();
        }
        return projectIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .collect(Collectors.toList());
    }
    
    private void validateProjects(List<Long> projectIds) {
        if (CollectionUtils.isEmpty(projectIds)) {
            return;
        }
        List<Project> projects = projectMapper.selectBatchIds(projectIds);
        if (projects.size() != projectIds.size()) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
    }
}

