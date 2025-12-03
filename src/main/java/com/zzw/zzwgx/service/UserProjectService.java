package com.zzw.zzwgx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzw.zzwgx.entity.UserProject;

import java.util.List;

/**
 * 用户项目关联服务
 */
public interface UserProjectService extends IService<UserProject> {
    
    /**
     * 查询用户管理的项目ID列表
     */
    List<Long> getProjectIdsByUser(Long userId);
    
    /**
     * 分配用户管理的项目
     */
    void assignProjects(Long userId, List<Long> projectIds);
}

