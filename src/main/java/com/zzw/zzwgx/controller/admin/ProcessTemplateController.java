package com.zzw.zzwgx.controller.admin;

import com.zzw.zzwgx.common.Result;
import com.zzw.zzwgx.common.enums.ResultCode;
import com.zzw.zzwgx.dto.request.CreateProcessTemplateBatchRequest;
import com.zzw.zzwgx.dto.request.CreateProcessTemplateRequest;
import com.zzw.zzwgx.dto.request.UpdateProcessTemplateRequest;
import com.zzw.zzwgx.dto.response.ProcessTemplateOptionResponse;
import com.zzw.zzwgx.dto.response.ProcessTemplateResponse;
import com.zzw.zzwgx.dto.response.TemplateListResponse;
import com.zzw.zzwgx.dto.response.TemplateWithProcessesResponse;
import com.zzw.zzwgx.entity.ProcessTemplate;
import com.zzw.zzwgx.entity.Template;
import com.zzw.zzwgx.entity.TemplateProcess;
import com.zzw.zzwgx.mapper.TemplateMapper;
import com.zzw.zzwgx.mapper.TemplateProcessMapper;
import com.zzw.zzwgx.service.ProcessTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 工序模板管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class ProcessTemplateController {
    
    private final ProcessTemplateService processTemplateService;
    private final TemplateMapper templateMapper;
    private final TemplateProcessMapper templateProcessMapper;
    
    @Operation(summary = "获取所有模板名称列表", description = "获取系统中所有工序模板的名称列表（去重）。如果传入工点ID，则只返回该工点下的模板名称。", tags = {"管理员管理-工序模板管理"})
    @GetMapping("/process-templates/names")
    public Result<List<String>> getAllTemplateNames(
            @Parameter(description = "工点ID（可选，传入则只返回该工点下的模板名称）", example = "1") @RequestParam(required = false) Long siteId) {
        log.info("查询模板名称列表，工点ID: {}", siteId);
        List<String> templateNames = siteId != null 
                ? processTemplateService.getTemplateNamesBySiteId(siteId)
                : processTemplateService.getAllTemplateNames();
        return Result.success(templateNames);
    }
    
    @Operation(summary = "获取模板列表", description = "获取模板列表，用于前端选择模板。如果传入工点ID，则只返回该工点下的模板。每个模板包含模板名称和对应的templateId（该模板下第一个工序模板的ID），前端选择模板后可以使用返回的templateId来创建循环。", tags = {"管理员管理-工序模板管理"})
    @GetMapping("/templates")
    public Result<List<TemplateListResponse>> getTemplateList(
            @Parameter(description = "工点ID（可选，传入则只返回该工点下的模板）", example = "1") @RequestParam(required = false) Long siteId) {
        log.info("查询模板列表，工点ID: {}", siteId);
        List<TemplateListResponse> templateList = siteId != null 
                ? processTemplateService.getTemplateListBySiteId(siteId)
                : processTemplateService.getTemplateList();
        return Result.success(templateList);
    }
    
    @Operation(summary = "获取所有模板及其工序列表", description = "获取系统中所有模板及其下的所有工序列表。返回每个模板的详细信息，包括模板名称、模板ID和该模板下的所有工序（按默认顺序排序）。", tags = {"管理员管理-工序模板管理"})
    @GetMapping("/templates/with-processes")
    public Result<List<TemplateWithProcessesResponse>> getTemplatesWithProcesses() {
        log.info("查询所有模板及其工序列表");
        List<TemplateWithProcessesResponse> templates = processTemplateService.getTemplatesWithProcesses();
        return Result.success(templates);
    }
    
    @Operation(summary = "获取所有工序模板选项列表", description = "获取系统中所有工序模板的选项列表，用于前端下拉选择工序名称。返回数据包含模板ID、工序名称、模板名称和控制时间等信息。前端选择工序名称后，可以使用返回的templateId来创建工序。", tags = {"管理员管理-工序模板管理"})
    @GetMapping("/process-templates/options")
    public Result<List<ProcessTemplateOptionResponse>> getAllProcessTemplateOptions() {
        log.info("查询所有工序模板选项列表");
        List<ProcessTemplateOptionResponse> options = processTemplateService.getAllProcessTemplateOptions();
        return Result.success(options);
    }
    
    @Operation(summary = "根据模板名称获取工序模板列表", description = "根据模板名称查询该模板下的所有工序定义，按默认顺序排序。如果传入工点ID，则只返回该工点下的模板。", tags = {"管理员管理-工序模板管理"})
    @GetMapping("/process-templates")
    public Result<List<ProcessTemplateResponse>> getProcessTemplates(
            @Parameter(description = "模板名称", required = true, example = "标准模板") @RequestParam String templateName,
            @Parameter(description = "工点ID（可选，传入则只返回该工点下的模板）", example = "1") @RequestParam(required = false) Long siteId) {
        log.info("查询工序模板列表，模板名称: {}, 工点ID: {}", templateName, siteId);
        List<ProcessTemplate> templates = siteId != null 
                ? processTemplateService.getTemplatesByNameAndSiteId(templateName, siteId)
                : processTemplateService.getTemplatesByName(templateName);
        List<ProcessTemplateResponse> responses = templates.stream()
                .map(processTemplateService::convertToResponse)
                .collect(Collectors.toList());
        return Result.success(responses);
    }
    
    @Operation(summary = "获取工序模板详情", description = "根据模板ID获取单个工序模板的详细信息。", tags = {"管理员管理-工序模板管理"})
    @GetMapping("/process-templates/{templateId}")
    public Result<ProcessTemplateResponse> getProcessTemplateDetail(
            @Parameter(description = "模板ID（template_process表的id）", required = true, example = "1") @PathVariable Long templateId) {
        log.info("查询工序模板详情，模板ID: {}", templateId);
        // templateId 现在是 template_process 表的 id
        TemplateProcess templateProcess = templateProcessMapper.selectById(templateId);
        if (templateProcess == null) {
            return Result.fail(ResultCode.TEMPLATE_NOT_FOUND);
        }
        
        // 查询模板信息
        Template template = templateMapper.selectById(templateProcess.getTemplateId());
        if (template == null) {
            return Result.fail(ResultCode.TEMPLATE_NOT_FOUND);
        }
        
        // 转换为 ProcessTemplate 对象（为了兼容接口）
        ProcessTemplate pt = new ProcessTemplate();
        pt.setId(templateProcess.getId());
        pt.setTemplateId(templateProcess.getTemplateId());
        pt.setTemplateName(template.getTemplateName());
        pt.setProcessCatalogId(templateProcess.getProcessCatalogId());
        pt.setControlTime(templateProcess.getControlTime());
        pt.setDefaultOrder(templateProcess.getDefaultOrder());
        pt.setDescription(templateProcess.getDescription());
        
        ProcessTemplateResponse response = processTemplateService.convertToResponse(pt);
        return Result.success(response);
    }
    
    @Operation(summary = "创建工序模板", description = "创建新的工序模板项，需选择工点和工序字典ID。", tags = {"管理员管理-工序模板管理"})
    @PostMapping("/process-templates")
    public Result<ProcessTemplateResponse> createProcessTemplate(@Valid @RequestBody CreateProcessTemplateRequest request) {
        log.info("创建工序模板，工点ID: {}, 模板名称: {}, 工序字典ID: {}", request.getSiteId(), request.getTemplateName(), request.getProcessCatalogId());
        ProcessTemplateResponse response = processTemplateService.createProcessTemplate(request);
        return Result.success(response);
    }
    
    @Operation(summary = "批量创建工序模板", description = "一次性为同一个模板名称创建多条工序模板，避免逐条新增。", tags = {"管理员管理-工序模板管理"})
    @PostMapping("/process-templates/batch")
    public Result<List<ProcessTemplateResponse>> createProcessTemplatesBatch(@Valid @RequestBody CreateProcessTemplateBatchRequest request) {
        log.info("批量创建工序模板，工点ID: {}, 模板名称: {}, 工序数量: {}", request.getSiteId(), request.getTemplateName(), request.getProcesses().size());
        List<ProcessTemplateResponse> responses = processTemplateService.createProcessTemplatesBatch(request);
        return Result.success(responses);
    }
    
    @Operation(summary = "更新工序模板", description = "更新指定工序模板的信息，支持部分字段更新。", tags = {"管理员管理-工序模板管理"})
    @PutMapping("/process-templates/{templateId}")
    public Result<ProcessTemplateResponse> updateProcessTemplate(
            @Parameter(description = "模板ID", required = true, example = "1") @PathVariable Long templateId,
            @Valid @RequestBody UpdateProcessTemplateRequest request) {
        log.info("更新工序模板，模板ID: {}", templateId);
        ProcessTemplateResponse response = processTemplateService.updateProcessTemplate(templateId, request);
        return Result.success(response);
    }
}

