package com.zzw.zzwgx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
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
    @Transactional(rollbackFor = Exception.class)
    public void assignProjects(Long userId, List<Long> projectIds) {
        log.info("分配用户项目，用户ID: {}, 项目IDs: {}", userId, projectIds);
        validateUserExist(userId);
        List<Long> normalizedIds = normalizeProjectIds(projectIds);
        validateProjects(normalizedIds);
        
        // 清空原有关系
        remove(new LambdaQueryWrapper<UserProject>().eq(UserProject::getUserId, userId));
        log.debug("已清空用户原有项目关联记录，用户ID: {}", userId);
        
        if (CollectionUtils.isEmpty(normalizedIds)) {
            return;
        }
        
        List<UserProject> relations = normalizedIds.stream().map(projectId -> {
            UserProject relation = new UserProject();
            relation.setUserId(userId);
            relation.setProjectId(projectId);
            return relation;
        }).collect(Collectors.toList());
        
        saveBatch(relations);
        log.info("用户项目分配成功，用户ID: {}, 关联数量: {}", userId, relations.size());
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

