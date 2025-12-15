package com.zzw.zzwgx.controller.admin;

import com.zzw.zzwgx.common.Result;
import com.zzw.zzwgx.dto.request.CreateProcessCatalogRequest;
import com.zzw.zzwgx.dto.request.UpdateProcessCatalogOrderRequest;
import com.zzw.zzwgx.dto.request.UpdateProcessCatalogRequest;
import com.zzw.zzwgx.dto.response.ProcessCatalogResponse;
import com.zzw.zzwgx.service.ProcessCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工序字典管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class ProcessCatalogController {
    
    private final ProcessCatalogService processCatalogService;
    
    @Operation(summary = "获取工序字典列表", description = "获取所有工序字典列表，按显示顺序排序。用于管理员查看和管理所有可用的工序。", tags = {"管理员管理-工序字典管理"})
    @GetMapping("/process-catalogs")
    public Result<List<ProcessCatalogResponse>> getProcessCatalogs() {
        log.info("查询工序字典列表");
        List<ProcessCatalogResponse> catalogs = processCatalogService.getAllProcessCatalogs();
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

