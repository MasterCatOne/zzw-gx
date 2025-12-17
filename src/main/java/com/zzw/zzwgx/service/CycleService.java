package com.zzw.zzwgx.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zzw.zzwgx.dto.request.CreateCycleRequest;
import com.zzw.zzwgx.dto.request.UpdateCycleRequest;
import com.zzw.zzwgx.dto.response.CycleReportDataResponse;
import com.zzw.zzwgx.dto.response.CycleResponse;
import com.zzw.zzwgx.dto.response.TemplateControlDurationResponse;
import com.zzw.zzwgx.entity.Cycle;

/**
 * 循环服务接口
 */
public interface CycleService extends IService<Cycle> {
    
    /**
     * 创建循环
     */
    CycleResponse createCycle(CreateCycleRequest request);
    
    /**
     * 更新循环
     */
    CycleResponse updateCycle(Long cycleId, UpdateCycleRequest request);
    
    /**
     * 获取循环详情
     */
    CycleResponse getCycleDetail(Long cycleId);
    
    /**
     * 分页查询项目下的循环
     */
    Page<CycleResponse> getCyclesByProject(Long projectId, Integer pageNum, Integer pageSize);
    
    /**
     * 根据项目ID获取当前循环
     */
    Cycle getCurrentCycleByProjectId(Long projectId);
    
    /**
     * 根据项目ID获取最新循环
     */
    Cycle getLatestCycleByProjectId(Long projectId);

    /**
     * 根据项目ID和循环号获取循环
     */
    Cycle getCycleByProjectAndNumber(Long projectId, Integer cycleNumber);

    /**
     * 导出循环报表（基于Excel模板填充）
     *
     * @param cycleId  循环ID
     * @param response HTTP响应流（写出Excel）
     */
    void exportCycleReport(Long cycleId, jakarta.servlet.http.HttpServletResponse response);
    
    /**
     * 获取循环报表数据（返回报表中需要填写的单元格值）
     *
     * @param cycleId 循环ID
     * @return 报表数据
     */
    CycleReportDataResponse getCycleReportData(Long cycleId);
    
    /**
     * 根据模板ID和工点ID获取模板的控制时长（所有工序的控制时间总和）
     * 用于创建循环页面，在创建循环前显示控制时长
     *
     * @param templateId 模板ID（该模板下任意一个工序模板的ID）
     * @param projectId 工点ID
     * @return 模板控制时长响应
     */
    TemplateControlDurationResponse getTemplateControlDuration(Long templateId);

    /**
     * 删除循环，并同时删除该循环下的所有工序（逻辑删除）
     *
     * @param cycleId 循环ID
     */
    void deleteCycle(Long cycleId);
}

