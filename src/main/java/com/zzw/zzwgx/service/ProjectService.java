package com.zzw.zzwgx.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zzw.zzwgx.dto.response.ProjectListResponse;
import com.zzw.zzwgx.dto.response.ProgressDetailResponse;
import com.zzw.zzwgx.entity.Project;

/**
 * 项目服务接口
 */
public interface ProjectService extends IService<Project> {
    
    /**
     * 分页查询项目列表
     */
    Page<Project> getProjectPage(Integer pageNum, Integer pageSize, String name);
    
    /**
     * 获取项目列表（返回Response）
     */
    Page<ProjectListResponse> getProjectList(Integer pageNum, Integer pageSize, String name);
    
    /**
     * 获取项目进度详情
     */
    ProgressDetailResponse getProgressDetail(Long projectId);
}

