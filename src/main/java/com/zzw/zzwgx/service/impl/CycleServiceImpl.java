package com.zzw.zzwgx.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.handler.SheetWriteHandler;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteWorkbookHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzw.zzwgx.common.enums.ProcessStatus;
import com.zzw.zzwgx.common.enums.ResultCode;
import com.zzw.zzwgx.common.exception.BusinessException;
import com.zzw.zzwgx.dto.request.CreateCycleRequest;
import com.zzw.zzwgx.dto.request.UpdateCycleRequest;
import com.zzw.zzwgx.dto.response.CycleResponse;
import com.zzw.zzwgx.entity.Cycle;
import com.zzw.zzwgx.entity.Process;
import com.zzw.zzwgx.entity.ProcessCatalog;
import com.zzw.zzwgx.entity.ProcessTemplate;
import com.zzw.zzwgx.entity.Project;
import com.zzw.zzwgx.mapper.CycleMapper;
import com.zzw.zzwgx.mapper.ProjectMapper;
import com.zzw.zzwgx.service.CycleService;
import com.zzw.zzwgx.service.ProcessCatalogService;
import com.zzw.zzwgx.service.ProcessService;
import com.zzw.zzwgx.service.ProcessTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 循环服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CycleServiceImpl extends ServiceImpl<CycleMapper, Cycle> implements CycleService {
    
    private final ProjectMapper projectMapper;
    private final ProcessTemplateService processTemplateService;
    private final ProcessCatalogService processCatalogService;
    private final ProcessService processService;
    
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CycleResponse createCycle(CreateCycleRequest request) {
        log.info("开始创建循环，项目ID: {}, 模板ID: {}", request.getProjectId(), request.getTemplateId());
        // 验证项目是否存在 - 直接使用Mapper避免循环依赖
        Project project = projectMapper.selectById(request.getProjectId());
        if (project == null) {
            log.error("创建循环失败，项目不存在，项目ID: {}", request.getProjectId());
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }

        // 根据templateId获取模板，然后根据模板名称获取该模板下的所有工序模板
        ProcessTemplate template = processTemplateService.getById(request.getTemplateId());
        if (template == null) {
            log.error("创建循环失败，工序模板不存在，模板ID: {}", request.getTemplateId());
            throw new BusinessException(ResultCode.TEMPLATE_NOT_FOUND);
        }
        
        // 根据模板名称获取该模板下的所有工序模板
        List<ProcessTemplate> templates = processTemplateService.getTemplatesByName(template.getTemplateName());
        if (templates.isEmpty()) {
            log.error("创建循环失败，模板下没有工序定义，模板名称: {}", template.getTemplateName());
            throw new BusinessException(ResultCode.TEMPLATE_NOT_FOUND);
        }

        // 业务校验：检查该工点是否已有进行中的循环
        Cycle existingCycle = getCurrentCycleByProjectId(request.getProjectId());
        if (existingCycle != null) {
            log.error("创建循环失败，该工点已有进行中的循环，项目ID: {}, 当前循环ID: {}, 循环号: {}", 
                    request.getProjectId(), existingCycle.getId(), existingCycle.getCycleNumber());
            throw new BusinessException(ResultCode.CYCLE_IN_PROGRESS_EXISTS);
        }
        
        // 获取当前循环次数
        Cycle latestCycle = getLatestCycleByProjectId(request.getProjectId());
        int cycleNumber = latestCycle != null ? latestCycle.getCycleNumber() + 1 : 1;
        log.debug("计算循环次数，项目ID: {}, 当前循环次数: {}", request.getProjectId(), cycleNumber);
        
        // 创建循环
        Cycle cycle = new Cycle();
        cycle.setProjectId(request.getProjectId());
        cycle.setCycleNumber(cycleNumber);
        cycle.setControlDuration(request.getControlDuration());
        cycle.setStartDate(request.getStartDate());
        cycle.setEndDate(request.getEndDate());
        
        // 预估开始时间与实际开始时间一致
        cycle.setEstimatedStartDate(request.getStartDate());
        log.debug("设置预估开始时间，与实际开始时间一致: {}", request.getStartDate());
        
        // 根据实际开始时间和控制时长标准自动计算预计结束时间
        if (request.getEstimatedEndDate() != null) {
            // 如果请求中已提供预计结束时间，使用提供的值
            cycle.setEstimatedEndDate(request.getEstimatedEndDate());
        } else if (request.getStartDate() != null && request.getControlDuration() != null) {
            // 如果没有提供预计结束时间，根据实际开始时间 + 控制时长（分钟）计算
            cycle.setEstimatedEndDate(request.getStartDate().plusMinutes(request.getControlDuration()));
            log.debug("自动计算预计结束时间，开始时间: {}, 控制时长: {}分钟, 预计结束时间: {}", 
                    request.getStartDate(), request.getControlDuration(), cycle.getEstimatedEndDate());
        } else {
            // 如果都没有提供，设置为null
            cycle.setEstimatedEndDate(request.getEstimatedEndDate());
        }
        if (request.getEstimatedMileage() != null) {
            cycle.setEstimatedMileage(BigDecimal.valueOf(request.getEstimatedMileage()));
        }
        cycle.setAdvanceLength(request.getAdvanceLength() != null
                ? BigDecimal.valueOf(request.getAdvanceLength())
                : BigDecimal.ZERO);
        cycle.setRockLevel(request.getRockLevel());
        cycle.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "IN_PROGRESS");
        save(cycle);
        log.info("循环创建成功，循环ID: {}, 循环次数: {}", cycle.getId(), cycleNumber);
        
        // 根据模板自动创建工序（模板已验证，直接创建）
        createProcessesFromTemplate(cycle.getId(), template.getTemplateName());
        
        return convertToResponse(cycle);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CycleResponse updateCycle(Long cycleId, UpdateCycleRequest request) {
        log.info("更新循环信息，循环ID: {}", cycleId);
        Cycle cycle = getById(cycleId);
        if (cycle == null) {
            throw new BusinessException(ResultCode.CYCLE_NOT_FOUND);
        }
        
        // 业务校验：如果要更新为 IN_PROGRESS 状态，检查该工点是否已有其他进行中的循环
        if (StringUtils.hasText(request.getStatus()) && "IN_PROGRESS".equals(request.getStatus())) {
            Cycle existingCycle = getCurrentCycleByProjectId(cycle.getProjectId());
            if (existingCycle != null && !existingCycle.getId().equals(cycleId)) {
                log.error("更新循环失败，该工点已有其他进行中的循环，项目ID: {}, 当前循环ID: {}, 循环号: {}", 
                        cycle.getProjectId(), existingCycle.getId(), existingCycle.getCycleNumber());
                throw new BusinessException(ResultCode.CYCLE_IN_PROGRESS_EXISTS);
            }
        }
        
        if (request.getControlDuration() != null) {
            cycle.setControlDuration(request.getControlDuration());
        }
        if (request.getStartDate() != null) {
            cycle.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            cycle.setEndDate(request.getEndDate());
        }
        if (request.getEstimatedStartDate() != null) {
            cycle.setEstimatedStartDate(request.getEstimatedStartDate());
        }
        if (request.getEstimatedEndDate() != null) {
            cycle.setEstimatedEndDate(request.getEstimatedEndDate());
        }
        if (request.getEstimatedMileage() != null) {
            cycle.setEstimatedMileage(request.getEstimatedMileage());
        }
        if (request.getActualMileage() != null) {
            cycle.setActualMileage(request.getActualMileage());
        }
        if (request.getAdvanceLength() != null) {
            cycle.setAdvanceLength(request.getAdvanceLength());
        }
        if (StringUtils.hasText(request.getStatus())) {
            cycle.setStatus(request.getStatus());
        }
        if (StringUtils.hasText(request.getRockLevel())) {
            cycle.setRockLevel(request.getRockLevel());
        }
        updateById(cycle);
        log.info("循环更新完成，循环ID: {}", cycleId);
        return convertToResponse(cycle);
    }
    
    @Override
    public CycleResponse getCycleDetail(Long cycleId) {
        Cycle cycle = getById(cycleId);
        if (cycle == null) {
            throw new BusinessException(ResultCode.CYCLE_NOT_FOUND);
        }
        return convertToResponse(cycle);
    }
    
    @Override
    public Page<CycleResponse> getCyclesByProject(Long projectId, Integer pageNum, Integer pageSize) {
        log.info("分页查询循环列表，项目ID: {}, 页码: {}, 大小: {}", projectId, pageNum, pageSize);
        Page<Cycle> page = page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Cycle>()
                        .eq(Cycle::getProjectId, projectId)
                        .orderByDesc(Cycle::getCycleNumber));
        Page<CycleResponse> responsePage = new Page<>(pageNum, pageSize, page.getTotal());
        responsePage.setRecords(page.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList()));
        return responsePage;
    }
    
    @Override
    public Cycle getCurrentCycleByProjectId(Long projectId) {
        log.debug("查询项目当前循环，项目ID: {}", projectId);
        LambdaQueryWrapper<Cycle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cycle::getProjectId, projectId)
                .eq(Cycle::getStatus, "IN_PROGRESS")
                .orderByDesc(Cycle::getCycleNumber)
                .last("LIMIT 1");
        Cycle cycle = getOne(wrapper);
        if (cycle != null) {
            log.debug("查询到当前循环，项目ID: {}, 循环ID: {}, 循环次数: {}", projectId, cycle.getId(), cycle.getCycleNumber());
        } else {
            log.debug("未查询到当前循环，项目ID: {}", projectId);
        }
        return cycle;
    }
    
    @Override
    public Cycle getLatestCycleByProjectId(Long projectId) {
        log.debug("查询项目最新循环，项目ID: {}", projectId);
        LambdaQueryWrapper<Cycle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cycle::getProjectId, projectId)
                .orderByDesc(Cycle::getCycleNumber)
                .last("LIMIT 1");
        Cycle cycle = getOne(wrapper);
        if (cycle != null) {
            log.debug("查询到最新循环，项目ID: {}, 循环ID: {}, 循环次数: {}", projectId, cycle.getId(), cycle.getCycleNumber());
        }
        return cycle;
    }

    @Override
    public Cycle getCycleByProjectAndNumber(Long projectId, Integer cycleNumber) {
        log.debug("根据项目和循环号查询循环，项目ID: {}, 循环号: {}", projectId, cycleNumber);
        if (cycleNumber == null) {
            return null;
        }
        LambdaQueryWrapper<Cycle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cycle::getProjectId, projectId)
                .eq(Cycle::getCycleNumber, cycleNumber)
                .last("LIMIT 1");
        return getOne(wrapper);
    }
    
    /**
     * 根据模板名称创建工序
     * 此方法假设模板已验证存在，不再进行重复验证
     */
    private void createProcessesFromTemplate(Long cycleId, String templateName) {
        log.info("根据模板创建工序，循环ID: {}, 模板名称: {}", cycleId, templateName);
        
        // 获取该模板下的所有工序模板（已在createCycle中验证，这里直接获取）
        List<ProcessTemplate> templates = processTemplateService.getTemplatesByName(templateName);
        
        // 根据模板创建工序
        for (ProcessTemplate processTemplate : templates) {
            Process process = new Process();
            process.setCycleId(cycleId);
            // 从工序字典获取工序名称（如果processTemplate有processCatalogId）
            if (processTemplate.getProcessCatalogId() != null) {
                ProcessCatalog catalog = processCatalogService.getById(processTemplate.getProcessCatalogId());
                if (catalog != null) {
                    process.setProcessName(catalog.getProcessName());
                } else {
                    process.setProcessName(processTemplate.getProcessName()); // 向后兼容
                }
                process.setProcessCatalogId(processTemplate.getProcessCatalogId());
            } else {
                process.setProcessName(processTemplate.getProcessName()); // 向后兼容
            }
            process.setControlTime(processTemplate.getControlTime());
            process.setStartOrder(processTemplate.getDefaultOrder());
            process.setProcessStatus(ProcessStatus.NOT_STARTED.getCode());
            process.setAdvanceLength(BigDecimal.ZERO);
            // 记录工序来源模板ID
            process.setTemplateId(processTemplate.getId());
            processService.save(process);
            log.debug("根据模板创建工序成功，工序名称: {}, 顺序: {}, 模板ID: {}", process.getProcessName(), processTemplate.getDefaultOrder(), processTemplate.getId());
        }
        
        log.info("根据模板创建工序完成，循环ID: {}, 创建工序数量: {}", cycleId, templates.size());
    }
    
    @Override
    public void exportCycleReport(Long cycleId, jakarta.servlet.http.HttpServletResponse response) {
        log.info("导出循环报表，循环ID: {}", cycleId);
        Cycle cycle = getById(cycleId);
        if (cycle == null) {
            throw new BusinessException(ResultCode.CYCLE_NOT_FOUND);
        }
        Project project = projectMapper.selectById(cycle.getProjectId());
        String projectName = project != null ? project.getProjectName() : "循环报表";
        
        // 计算时间信息
        LocalDateTime start = cycle.getStartDate();
        LocalDateTime end = cycle.getEndDate();
        Integer controlMinutes = cycle.getControlDuration();
        String controlDurationStr = formatDuration(controlMinutes);
        Double controlHours = controlMinutes != null ? controlMinutes / 60.0 : null;
        
        long elapsedMinutes = 0;
        if (start != null) {
            LocalDateTime endPoint = end != null ? end : LocalDateTime.now();
            elapsedMinutes = Duration.between(start, endPoint).toMinutes();
        }
        String elapsedStr = formatDuration((int) elapsedMinutes);
        
        String remainingStr = "";
        if (controlMinutes != null) {
            long remain = controlMinutes - elapsedMinutes;
            remainingStr = formatDuration((int) remain);
        }
        
        String predictedNextByControl = "";
        if (start != null && controlMinutes != null) {
            predictedNextByControl = start.plusMinutes(controlMinutes).format(DATETIME_FORMATTER);
        }
        String predictedNextByInterval = "";
        if (end != null && controlMinutes != null) {
            predictedNextByInterval = end.plusMinutes(controlMinutes).format(DATETIME_FORMATTER);
        }
        
        // 当前施工状态：取进行中的工序名称
        List<Process> processes = processService.getProcessesByCycleId(cycleId);
        String currentStatus = processes.stream()
                .filter(p -> ProcessStatus.IN_PROGRESS.getCode().equals(p.getProcessStatus()))
                .map(Process::getProcessName)
                .sorted()
                .collect(Collectors.joining("、"));
        if (!StringUtils.hasText(currentStatus)) {
            // 如果没有进行中的，取默认顺序最前的工序名称作提示
            Optional<Process> latest = processes.stream()
                    .sorted(Comparator.comparing(Process::getStartOrder, Comparator.nullsLast(Integer::compareTo)))
                    .findFirst();
            currentStatus = latest.map(Process::getProcessName).orElse("");
        }
        
        // 模板路径：优先读取 xlxs 目录下的第一个 xlsx
        Path templatePath = findTemplatePath();
        if (templatePath == null) {
            throw new BusinessException("未找到报表模板文件（xlxs目录下的xlsx）");
        }
        
        try {
            Map<String, TemplateCellValue> cellValues = new HashMap<>();
            // 顶部概要
            cellValues.put("D2", TemplateCellValue.string(projectName + "循环时间通报" + (start != null ? "(" + start.toLocalDate() + ")" : "")));
            cellValues.put("C2", TemplateCellValue.date(start, false));
            cellValues.put("F2", TemplateCellValue.date(end, false));
            cellValues.put("I2", TemplateCellValue.string(elapsedStr));
            cellValues.put("K2", TemplateCellValue.number(controlMinutes));
            cellValues.put("L2", TemplateCellValue.number(controlHours));
            cellValues.put("O2", TemplateCellValue.number(cycle.getCycleNumber()));
            
            // 里程、围岩、进尺、控制标准
            cellValues.put("F5", TemplateCellValue.string(formatMileage(cycle)));
            cellValues.put("D4", TemplateCellValue.string(cycle.getRockLevel()));
            cellValues.put("K3", TemplateCellValue.string(cycle.getAdvanceLength() != null ? cycle.getAdvanceLength().toPlainString() : ""));
            cellValues.put("O3", TemplateCellValue.string(controlDurationStr));
            cellValues.put("F3", TemplateCellValue.string(controlDurationStr));
            
            // 时间节点
            cellValues.put("K5", TemplateCellValue.date(parseDateTime(predictedNextByControl), true));
            cellValues.put("L3", TemplateCellValue.string(remainingStr));
            cellValues.put("I5", TemplateCellValue.date(parseDateTime(predictedNextByInterval), true));
            
            // 当前施工状态
            cellValues.put("C7", TemplateCellValue.string(currentStatus));
            
            String fileName = projectName + "-循环报表.xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8) + "\"");
            
            // 先写入内存，确保文件完整，再输出到响应流，避免被中途截断导致“文件损坏”
            try (java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                 ExcelWriter writer = EasyExcel.write(bos)
                         .withTemplate(templatePath.toFile())
                         .inMemory(true) // 使用内存模式，避免 SXSSF 对已存在行的限制
                         .autoCloseStream(false)
                         .registerWriteHandler(new TemplateCellWriteHandler(cellValues))
                         .build()) {
                WriteSheet sheet = EasyExcel.writerSheet(0).build();
                // 写入空数据以触发模板和处理器
                writer.fill(new ArrayList<>(), sheet);
                writer.finish();
                
                // 输出到响应流
                byte[] data = bos.toByteArray();
                response.setContentLength(data.length);
                try (var out = response.getOutputStream()) {
                    out.write(data);
                    out.flush();
                }
            }
            response.flushBuffer();
        } catch (IOException e) {
            log.error("导出循环报表失败，循环ID: {}", cycleId, e);
            throw new BusinessException("导出循环报表失败");
        }
    }
    
    private Path findTemplatePath() {
        try {
            Path dir = Paths.get("xlxs");
            if (!Files.exists(dir)) {
                return null;
            }
            return Files.list(dir)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".xlsx"))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            log.warn("查找报表模板文件失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 通过 EasyExcel 的 SheetWriteHandler 在模板创建后按坐标填充数据
     */
    private static class TemplateCellWriteHandler implements SheetWriteHandler {
        
        private final Map<String, TemplateCellValue> cellValues;
        
        TemplateCellWriteHandler(Map<String, TemplateCellValue> cellValues) {
            this.cellValues = cellValues;
        }
        
        @Override
        public void beforeSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
            // no-op
        }
        
        @Override
        public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
            Sheet sheet = writeSheetHolder.getSheet();
            if (sheet == null || cellValues == null || cellValues.isEmpty()) {
                return;
            }
            cellValues.forEach((ref, val) -> applyValue(sheet, ref, val));
        }
        
        private void applyValue(Sheet sheet, String cellRef, TemplateCellValue value) {
            if (value == null || cellRef == null) {
                return;
            }
            CellReference ref = new CellReference(cellRef);
            Row row = sheet.getRow(ref.getRow());
            if (row == null) {
                row = sheet.createRow(ref.getRow());
            }
            Cell cell = row.getCell(ref.getCol());
            if (cell == null) {
                cell = row.createCell(ref.getCol());
            }
            switch (value.type) {
                case STRING -> cell.setCellValue(value.stringValue == null ? "" : value.stringValue);
                case NUMBER -> cell.setCellValue(value.numberValue != null ? value.numberValue : 0d);
                case DATE -> {
                    if (value.dateValue != null) {
                        cell.setCellValue(value.dateValue);
                    }
                }
                default -> {
                }
            }
        }
    }
    
    private static class TemplateCellValue {
        enum Type {STRING, NUMBER, DATE}
        
        private final Type type;
        private final String stringValue;
        private final Double numberValue;
        private final java.util.Date dateValue;
        
        private TemplateCellValue(Type type, String stringValue, Double numberValue, java.util.Date dateValue) {
            this.type = type;
            this.stringValue = stringValue;
            this.numberValue = numberValue;
            this.dateValue = dateValue;
        }
        
        static TemplateCellValue string(String v) {
            return new TemplateCellValue(Type.STRING, v, null, null);
        }
        
        static TemplateCellValue number(Number v) {
            return v == null ? null : new TemplateCellValue(Type.NUMBER, null, v.doubleValue(), null);
        }
        
        static TemplateCellValue date(LocalDateTime v, boolean withTime) {
            if (v == null) {
                return null;
            }
            java.util.Date date = java.util.Date.from(v.atZone(ZoneId.systemDefault()).toInstant());
            return new TemplateCellValue(Type.DATE, null, null, date);
        }
    }
    
    private LocalDateTime parseDateTime(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return LocalDateTime.parse(text, DATETIME_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }
    
    private String formatMileage(Cycle cycle) {
        if (cycle == null) {
            return "";
        }
        if (cycle.getActualMileage() != null) {
            return cycle.getActualMileage().toPlainString();
        }
        if (cycle.getEstimatedMileage() != null) {
            return cycle.getEstimatedMileage().toPlainString();
        }
        return "";
    }
    
    private String formatDuration(Integer minutes) {
        if (minutes == null) {
            return "";
        }
        return formatDuration(minutes.longValue());
    }
    
    private String formatDuration(long minutes) {
        if (minutes < 0) {
            return "";
        }
        long hours = minutes / 60;
        long mins = minutes % 60;
        return hours + "小时" + mins + "分钟";
    }
    
    /**
     * 转换Cycle为CycleResponse
     */
    private CycleResponse convertToResponse(Cycle cycle) {
        if (cycle == null) {
            return null;
        }
        return BeanUtil.copyProperties(cycle, CycleResponse.class);
    }
}

