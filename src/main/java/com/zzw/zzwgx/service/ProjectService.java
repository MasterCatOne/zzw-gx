package com.zzw.zzwgx.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zzw.zzwgx.dto.request.ProjectRequest;
import com.zzw.zzwgx.dto.response.ProjectListResponse;
import com.zzw.zzwgx.dto.response.ProgressDetailResponse;
import com.zzw.zzwgx.dto.response.ProjectTreeNodeResponse;
import com.zzw.zzwgx.dto.response.SiteConstructionStatusResponse;
import com.zzw.zzwgx.entity.Project;

import java.util.List;

/**
 * 项目服务接口
 */
public interface ProjectService extends IService<Project> {
    
    /**
     * 分页查询项目列表
     */
    Page<Project> getProjectPage(Integer pageNum, Integer pageSize, String name);
    
    /**
     * 获取工点列表（返回Response）
     */
    Page<ProjectListResponse> getProjectList(Integer pageNum, Integer pageSize, String name, String status, Long userId);
    
    /**
     * 获取项目进度详情
     */
    ProgressDetailResponse getProgressDetail(Long projectId, Integer cycleNumber);
    
    /**
     * 获取完整项目树
     */
    List<ProjectTreeNodeResponse> getProjectTree();
    
    /**
     * 新增项目节点
     */
    ProjectTreeNodeResponse createProject(ProjectRequest request);
    
    /**
     * 编辑项目节点
     */
    ProjectTreeNodeResponse updateProject(Long projectId, ProjectRequest request);
    
    /**
     * 删除项目节点
     */
    void deleteProject(Long projectId);
    
    /**
     * 获取工点施工状态（管理员查看）
     */
    SiteConstructionStatusResponse getSiteConstructionStatus(Long projectId);
}

