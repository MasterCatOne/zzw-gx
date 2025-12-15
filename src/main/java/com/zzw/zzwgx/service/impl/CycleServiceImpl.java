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
import com.zzw.zzwgx.common.enums.RockLevel;
import com.zzw.zzwgx.common.exception.BusinessException;
import com.zzw.zzwgx.dto.request.CreateCycleRequest;
import com.zzw.zzwgx.dto.request.UpdateCycleRequest;
import com.zzw.zzwgx.dto.response.CycleReportDataResponse;
import com.zzw.zzwgx.dto.response.CycleResponse;
import com.zzw.zzwgx.dto.response.TemplateControlDurationResponse;
import com.zzw.zzwgx.entity.Cycle;
import com.zzw.zzwgx.entity.Process;
import com.zzw.zzwgx.entity.ProcessCatalog;
import com.zzw.zzwgx.entity.ProcessTemplate;
import com.zzw.zzwgx.entity.Project;
import com.zzw.zzwgx.mapper.CycleMapper;
import com.zzw.zzwgx.mapper.ProjectMapper;
import com.zzw.zzwgx.mapper.TemplateMapper;
import com.zzw.zzwgx.mapper.TemplateProcessMapper;
import com.zzw.zzwgx.mapper.ProjectTemplateMapper;
import com.zzw.zzwgx.entity.Template;
import com.zzw.zzwgx.entity.TemplateProcess;
import com.zzw.zzwgx.entity.ProjectTemplate;
import com.zzw.zzwgx.security.SecurityUtils;
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
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
    private final ResourceLoader resourceLoader;
    private final TemplateMapper templateMapper;
    private final TemplateProcessMapper templateProcessMapper;
    private final ProjectTemplateMapper projectTemplateMapper;

    private static final BigDecimal PROJECT_START_MILEAGE = new BigDecimal("84000");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
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

        // 根据templateId获取模板工序（template_process表）
        // templateId 现在是 template_process 表的 id
        TemplateProcess templateProcess = templateProcessMapper.selectById(request.getTemplateId());
        if (templateProcess == null) {
            log.error("创建循环失败，模板工序不存在，模板ID: {}", request.getTemplateId());
            throw new BusinessException(ResultCode.TEMPLATE_NOT_FOUND);
        }
        
        // 根据 template_id 查询模板信息
        Template template = templateMapper.selectById(templateProcess.getTemplateId());
        if (template == null) {
            log.error("创建循环失败，模板不存在，模板ID: {}", templateProcess.getTemplateId());
            throw new BusinessException(ResultCode.TEMPLATE_NOT_FOUND);
        }
        
        // 验证该工点是否关联了该模板
        ProjectTemplate projectTemplate = projectTemplateMapper.selectOne(new LambdaQueryWrapper<ProjectTemplate>()
                .eq(ProjectTemplate::getProjectId, request.getProjectId())
                .eq(ProjectTemplate::getTemplateId, template.getId())
                .eq(ProjectTemplate::getDeleted, 0)
                .last("LIMIT 1"));
        
        if (projectTemplate == null) {
            log.error("创建循环失败，该工点未关联该模板，工点ID: {}, 模板ID: {}", 
                    request.getProjectId(), template.getId());
            throw new BusinessException("该工点未关联该模板");
        }
        
        // 根据模板名称获取该模板下的所有工序模板
        List<ProcessTemplate> templates = processTemplateService.getTemplatesByName(template.getTemplateName());
        if (templates.isEmpty()) {
            log.error("创建循环失败，模板没有工序定义，模板ID: {}, 模板名称: {}", 
                    template.getId(), template.getTemplateName());
            throw new BusinessException(ResultCode.TEMPLATE_NOT_FOUND);
        }

        // 计算控制时长：始终根据模板中所有工序的控制时间总和
        Integer controlDuration = templates.stream()
                .filter(t -> t.getControlTime() != null && t.getControlTime() > 0)
                .mapToInt(ProcessTemplate::getControlTime)
                .sum();
        log.info("自动计算控制时长，模板名称: {}, 工序数量: {}, 控制时长总和: {}分钟",
                template.getTemplateName(), templates.size(), controlDuration);
        
        if (controlDuration == null || controlDuration <= 0) {
            log.error("创建循环失败，控制时长无效，工点ID: {}, 模板名称: {}, 控制时长: {}", 
                    request.getProjectId(), template.getTemplateName(), controlDuration);
            throw new BusinessException("模板中工序的控制时间总和无效，无法创建循环");
        }

        // 业务校验：检查该工点是否已有进行中的循环
//        Cycle existingCycle = getCurrentCycleByProjectId(request.getProjectId());
//        if (existingCycle != null) {
//            log.error("创建循环失败，该工点已有进行中的循环，项目ID: {}, 当前循环ID: {}, 循环号: {}",
//                    request.getProjectId(), existingCycle.getId(), existingCycle.getCycleNumber());
//            throw new BusinessException(ResultCode.CYCLE_IN_PROGRESS_EXISTS);
//        }
        
        // 验证开始时间不能是过去时间（允许3分钟内的误差）
        LocalDateTime now = LocalDateTime.now();
        if (request.getStartDate() != null && request.getStartDate().isBefore(now.minusMinutes(3))) {
            log.error("创建循环失败，开始时间不能是过去时间（超过3分钟误差），开始时间: {}, 当前时间: {}", 
                    request.getStartDate(), now);
            throw new BusinessException(ResultCode.CYCLE_START_TIME_INVALID);
        }
        
        // 获取当前循环次数
        Cycle latestCycle = getLatestCycleByProjectId(request.getProjectId());
        int cycleNumber = latestCycle != null ? latestCycle.getCycleNumber() + 1 : 1;
        log.debug("计算循环次数，项目ID: {}, 当前循环次数: {}", request.getProjectId(), cycleNumber);
        
        // 创建循环
        Cycle cycle = new Cycle();
        cycle.setProjectId(request.getProjectId());
        cycle.setCycleNumber(cycleNumber);
        cycle.setControlDuration(controlDuration);
        cycle.setRockLevel(RockLevel.LEVEL_I.getCode());
        cycle.setStartDate(request.getStartDate());
        cycle.setEndDate(request.getEndDate());
        cycle.setDevelopmentMethod(request.getDevelopmentMethod());
        
        // 预估开始时间与实际开始时间一致
        cycle.setEstimatedStartDate(request.getStartDate());
        log.debug("设置预估开始时间，与实际开始时间一致: {}", request.getStartDate());
        
        // 根据实际开始时间和控制时长标准自动计算预计结束时间
        if (request.getEstimatedEndDate() != null) {
            // 如果请求中已提供预计结束时间，使用提供的值
            cycle.setEstimatedEndDate(request.getEstimatedEndDate());
        } else if (request.getStartDate() != null && controlDuration != null) {
            // 如果没有提供预计结束时间，根据实际开始时间 + 控制时长（分钟）计算
            cycle.setEstimatedEndDate(request.getStartDate().plusMinutes(controlDuration));
            log.debug("自动计算预计结束时间，开始时间: {}, 控制时长: {}分钟, 预计结束时间: {}", 
                    request.getStartDate(), controlDuration, cycle.getEstimatedEndDate());
        } else {
            // 如果都没有提供，设置为null
            cycle.setEstimatedEndDate(request.getEstimatedEndDate());
        }
        if (request.getEstimatedMileage() != null) {
            cycle.setEstimatedMileage(BigDecimal.valueOf(request.getEstimatedMileage()));
        }
        // 实际里程创建时通常未知，保持null
        cycle.setAdvanceLength(request.getAdvanceLength() != null
                ? BigDecimal.valueOf(request.getAdvanceLength())
                : BigDecimal.ZERO);
        cycle.setRockLevel(request.getRockLevel());
        cycle.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "IN_PROGRESS");
        save(cycle);
        log.info("循环创建成功，循环ID: {}, 循环次数: {}", cycle.getId(), cycleNumber);
        
        // 根据模板自动创建工序（模板已验证，直接创建）
        Long currentUserId = SecurityUtils.getCurrentUserId();
        createProcessesFromTemplate(cycle.getId(), request.getProjectId(), template.getTemplateName(), cycle.getStartDate(), currentUserId);
        
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
        if (request.getDevelopmentMethod() != null) {
            cycle.setDevelopmentMethod(request.getDevelopmentMethod());
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
    private void createProcessesFromTemplate(Long cycleId, Long siteId, String templateName, LocalDateTime cycleStartTime, Long firstOperatorId) {
        log.info("根据模板创建工序，循环ID: {}, 工点ID: {}, 模板名称: {}", cycleId, siteId, templateName);
        
        // 获取该模板的所有工序模板（模板是全局的，不区分工点）
        List<ProcessTemplate> templates = processTemplateService.getTemplatesByName(templateName).stream()
                .sorted(Comparator.comparing(ProcessTemplate::getDefaultOrder, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
        
        // 根据模板创建工序
        boolean firstStarted = false;
        int createdCount = 0;
        for (ProcessTemplate processTemplate : templates) {
            // 如果工序模板的控制时间为0或null，则跳过该工序
            if (processTemplate.getControlTime() == null || processTemplate.getControlTime() == 0) {
                log.debug("跳过控制时间为0的工序模板，工序名称: {}, 顺序: {}, 模板ID: {}", 
                        processTemplate.getProcessName(), processTemplate.getDefaultOrder(), processTemplate.getId());
                continue;
            }
            
            Process process = new Process();
            process.setCycleId(cycleId);
            // 从工序字典获取工序名称（如果processTemplate有processCatalogId）
            if (processTemplate.getProcessCatalogId() != null) {
                ProcessCatalog catalog = processCatalogService.getById(processTemplate.getProcessCatalogId());
                if (catalog != null) {
                    process.setProcessName(catalog.getProcessName());
                    process.setCategory(catalog.getCategory());
                } else {
                    process.setProcessName(processTemplate.getProcessName()); // 向后兼容
                }
                process.setProcessCatalogId(processTemplate.getProcessCatalogId());
            } else {
                process.setProcessName(processTemplate.getProcessName()); // 向后兼容
            }
            process.setControlTime(processTemplate.getControlTime());
            process.setStartOrder(processTemplate.getDefaultOrder());
            process.setAdvanceLength(BigDecimal.ZERO);
            // 记录工序来源模板ID（template_process表的id）
            process.setTemplateId(processTemplate.getId());

            // 首个工序直接开工，操作员为当前用户；其余保持未开始
            if (!firstStarted && firstOperatorId != null) {
                process.setProcessStatus(ProcessStatus.IN_PROGRESS.getCode());
                process.setOperatorId(firstOperatorId);
                LocalDateTime actualStart = cycleStartTime != null ? cycleStartTime : LocalDateTime.now();
                process.setActualStartTime(actualStart);
                process.setEstimatedStartTime(actualStart);
                if (processTemplate.getControlTime() != null) {
                    process.setEstimatedEndTime(actualStart.plusMinutes(processTemplate.getControlTime()));
                }
                firstStarted = true;
            } else {
                process.setProcessStatus(ProcessStatus.NOT_STARTED.getCode());
                process.setOperatorId(null);
            }
            processService.save(process);
            createdCount++;
            log.debug("根据模板创建工序成功，工序名称: {}, 顺序: {}, 模板ID: {}", process.getProcessName(), processTemplate.getDefaultOrder(), processTemplate.getId());
        }
        
        log.info("根据模板创建工序完成，循环ID: {}, 模板工序总数: {}, 实际创建工序数量: {}", cycleId, templates.size(), createdCount);
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
        Double controlHours = controlMinutes != null ? controlMinutes / 60.0 : null;
        
        // 获取上循环信息
        Cycle lastCycle = getCycleByProjectAndNumber(cycle.getProjectId(), cycle.getCycleNumber() - 1);
        // 上循环响炮时间 = 上循环中"装药爆破"工序的结束时间
        LocalDateTime lastCycleBlastTime = lastCycle != null ? getBlastTimeByCycleId(lastCycle.getId()) : null;
        
        // 获取本循环响炮时间 = 本循环中"装药爆破"工序的结束时间
        LocalDateTime currentCycleBlastTime = getBlastTimeByCycleId(cycleId);
        
        // 计算预测时间
        LocalDateTime predictedNextByControl = null; // 本循环响炮时间+控制标准=预测下循环响炮时间
        if (currentCycleBlastTime != null && controlMinutes != null) {
            predictedNextByControl = currentCycleBlastTime.plusMinutes(controlMinutes);
        }
        
        LocalDateTime predictedNextByInterval = null; // 本循环响炮时间+本循环从响炮到结束的实际耗时=预测下循环响炮时间（按间隔）
        if (currentCycleBlastTime != null && end != null) {
            // 计算本循环从响炮到结束的实际耗时（分钟）
            long blastToEndMinutes = Duration.between(currentCycleBlastTime, end).toMinutes();
            predictedNextByInterval = currentCycleBlastTime.plusMinutes(blastToEndMinutes);
        }
        
        // 本循环理论响炮时间 = 上循环响炮时间 + 本循环控制标准
        LocalDateTime theoreticalBlastTime = null;
        if (lastCycleBlastTime != null && controlMinutes != null) {
            theoreticalBlastTime = lastCycleBlastTime.plusMinutes(controlMinutes);
        }
        
        // 响炮超时 = 本循环响炮时间 - 本循环理论响炮时间（分钟）
        Long blastOvertimeMinutes = null;
        if (currentCycleBlastTime != null && theoreticalBlastTime != null) {
            blastOvertimeMinutes = Duration.between(theoreticalBlastTime, currentCycleBlastTime).toMinutes();
        }
        
        // 两循环响炮时间差 = 本循环响炮时间 - 上循环响炮时间（分钟）
        Long cycleBlastDiffMinutes = null;
        if (currentCycleBlastTime != null && lastCycleBlastTime != null) {
            cycleBlastDiffMinutes = Duration.between(lastCycleBlastTime, currentCycleBlastTime).toMinutes();
        }
        
        // 获取工序列表
        List<Process> processes = processService.getProcessesByCycleId(cycleId);
        
        // 模板路径：从 static 目录读取模板文件
        File templateFile = findTemplateFile();
        if (templateFile == null || !templateFile.exists()) {
            throw new BusinessException("未找到报表模板文件（static目录下的xlsx）");
        }
        
        try {
            Map<String, TemplateCellValue> cellValues = new HashMap<>();
            // 标题行（A1-P1 合并单元格）：工点名称
            cellValues.put("A1", TemplateCellValue.string(projectName+"循环时间通报"));
            
            // 第2行
            cellValues.put("C2", TemplateCellValue.date(start, false)); // 循环开始时间
            // F2：循环结束时间（F2,G2,H2合并），如果没有结束日期则显示"施工中"
            if (end != null) {
                cellValues.put("F2", TemplateCellValue.date(end, false));
            } else {
                cellValues.put("F2", TemplateCellValue.string("施工中"));
            }
            cellValues.put("K2", TemplateCellValue.number(controlMinutes)); // 控制时长（分钟）
            cellValues.put("L2", TemplateCellValue.number(controlHours)); // 控制时长（小时）
            cellValues.put("O2", TemplateCellValue.number(cycle.getCycleNumber())); // 本月循环数（O2,P2合并）
            
            // 第3行
            // 掌子面里程：优先使用实际里程，其次预估里程，都没有则不填
            String mileage = "";
            if (cycle.getActualMileage() != null) {
                mileage = formatMileage(cycle.getActualMileage());
            } else if (cycle.getEstimatedMileage() != null) {
                mileage = formatMileage(cycle.getEstimatedMileage());
            }
            cellValues.put("C3", TemplateCellValue.string(mileage)); // 掌子面里程
            cellValues.put("F3", TemplateCellValue.string(RockLevel.LEVEL_I.getCode())); // 围岩等级（F3,G3,H3合并）
            cellValues.put("K3", TemplateCellValue.string(cycle.getAdvanceLength() != null ? cycle.getAdvanceLength().toPlainString() : "")); // 进尺（K3,L3合并）
            String devMethod = StringUtils.hasText(cycle.getDevelopmentMethod())
                    ? cycle.getDevelopmentMethod()
                    : "台阶法";
            cellValues.put("O3", TemplateCellValue.string(devMethod)); // 开发方式（O3,P3合并）
            
            // 第4行
            // 精确到秒，直接填充字符串
            cellValues.put("C4", TemplateCellValue.string(formatDateTime(lastCycleBlastTime))); // 上循环响炮时间
            cellValues.put("F4", TemplateCellValue.string(formatDateTime(theoreticalBlastTime))); // 本循环理论响炮时间（F4,G4,H4合并）
            // K4、L4：响炮超时（合并单元格），如果没有响炮时间则显示"数据缺失"，否则显示"X天X小时X分钟"
            if (currentCycleBlastTime == null) {
                cellValues.put("K4", TemplateCellValue.string("数据缺失"));
            } else if (blastOvertimeMinutes != null) {
                cellValues.put("K4", TemplateCellValue.string(formatDaysHoursMinutes(blastOvertimeMinutes)));
            } else {
                cellValues.put("K4", TemplateCellValue.string(""));
            }
            cellValues.put("N4", TemplateCellValue.number(cycleBlastDiffMinutes != null ? cycleBlastDiffMinutes.doubleValue() : null)); // 两循环响炮时间差（分钟）
            cellValues.put("O4", TemplateCellValue.string(formatMinutesFromLong(cycleBlastDiffMinutes))); // 两循环响炮时间差（X小时Y分钟，O4,P4合并）
            
            // 第5行
            // 需要精确到秒，直接填充为字符串
            cellValues.put("C5", TemplateCellValue.string(formatDateTime(currentCycleBlastTime))); // 循环实际响炮时间（装药爆破结束时间）
            cellValues.put("F5", TemplateCellValue.string(formatDateTime(predictedNextByControl))); // 预测下循环响炮时间（按控制标准，F5,G5,H5合并）
            cellValues.put("K5", TemplateCellValue.string(formatDateTime(predictedNextByInterval))); // 预测下循环响炮时间（按间隔，K5到P5合并）
            
            String fileName = projectName + "-循环报表.xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8) + "\"");
            
            // 先写入内存，确保文件完整，再输出到响应流，避免被中途截断导致"文件损坏"
            try (java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                 ExcelWriter writer = EasyExcel.write(bos)
                         .withTemplate(templateFile)
                         .inMemory(true) // 使用内存模式，避免 SXSSF 对已存在行的限制
                         .autoCloseStream(false)
                         .registerWriteHandler(new TemplateCellWriteHandler(cellValues, processes))
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
    
    @Override
    public CycleReportDataResponse getCycleReportData(Long cycleId) {
        log.info("获取循环报表数据，循环ID: {}", cycleId);
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
        Double controlHours = controlMinutes != null ? controlMinutes / 60.0 : null;
        
        // 获取上循环信息
        Cycle lastCycle = getCycleByProjectAndNumber(cycle.getProjectId(), cycle.getCycleNumber() - 1);
        // 上循环响炮时间 = 上循环中"装药爆破"工序的结束时间
        LocalDateTime lastCycleBlastTime = lastCycle != null ? getBlastTimeByCycleId(lastCycle.getId()) : null;
        
        // 获取本循环响炮时间 = 本循环中"装药爆破"工序的结束时间
        LocalDateTime currentCycleBlastTime = getBlastTimeByCycleId(cycleId);
        
        // 计算预测时间
        LocalDateTime predictedNextByControl = null; // 本循环响炮时间+控制标准=预测下循环响炮时间
        if (currentCycleBlastTime != null && controlMinutes != null) {
            predictedNextByControl = currentCycleBlastTime.plusMinutes(controlMinutes);
        }
        
        LocalDateTime predictedNextByInterval = null; // 本循环响炮时间+本循环从响炮到结束的实际耗时=预测下循环响炮时间（按间隔）
        if (currentCycleBlastTime != null && end != null) {
            // 计算本循环从响炮到结束的实际耗时（分钟）
            long blastToEndMinutes = Duration.between(currentCycleBlastTime, end).toMinutes();
            predictedNextByInterval = currentCycleBlastTime.plusMinutes(blastToEndMinutes);
        }
        
        // 本循环理论响炮时间 = 上循环响炮时间 + 本循环控制标准
        LocalDateTime theoreticalBlastTime = null;
        if (lastCycleBlastTime != null && controlMinutes != null) {
            theoreticalBlastTime = lastCycleBlastTime.plusMinutes(controlMinutes);
        }
        
        // 响炮超时 = 本循环响炮时间 - 本循环理论响炮时间（分钟）
        Long blastOvertimeMinutes = null;
        if (currentCycleBlastTime != null && theoreticalBlastTime != null) {
            blastOvertimeMinutes = Duration.between(theoreticalBlastTime, currentCycleBlastTime).toMinutes();
        }
        
        // 两循环响炮时间差 = 本循环响炮时间 - 上循环响炮时间（分钟）
        Long cycleBlastDiffMinutes = null;
        if (currentCycleBlastTime != null && lastCycleBlastTime != null) {
            cycleBlastDiffMinutes = Duration.between(lastCycleBlastTime, currentCycleBlastTime).toMinutes();
        }
        
        // 获取工序列表
        List<Process> processes = processService.getProcessesByCycleId(cycleId);
        
        // 构建响应对象
        CycleReportDataResponse response = new CycleReportDataResponse();
        response.setTitle(projectName + "循环时间通报");
        
        // 第2行数据
        CycleReportDataResponse.Row2Data row2 = new CycleReportDataResponse.Row2Data();
        row2.setCycleStartTime(start);
        row2.setCycleEndTime(end);
        row2.setControlDurationMinutes(controlMinutes);
        row2.setControlDurationHours(controlHours);
        row2.setCycleNumber(cycle.getCycleNumber());
        response.setRow2(row2);
        
        // 第3行数据
        CycleReportDataResponse.Row3Data row3 = new CycleReportDataResponse.Row3Data();
        // 掌子面里程：优先使用实际里程，其次预估里程，都没有则不填
        String mileage = "";
        if (cycle.getActualMileage() != null) {
            mileage = formatMileage(cycle.getActualMileage());
        } else if (cycle.getEstimatedMileage() != null) {
            mileage = formatMileage(cycle.getEstimatedMileage());
        }
        row3.setMileage(mileage);
        row3.setRockLevel(cycle.getRockLevel());
        row3.setAdvanceLength(cycle.getAdvanceLength() != null ? cycle.getAdvanceLength().toPlainString() : "");
        String devMethod = StringUtils.hasText(cycle.getDevelopmentMethod())
                ? cycle.getDevelopmentMethod()
                : "台阶法";
        row3.setDevelopmentMethod(devMethod);
        response.setRow3(row3);
        
        // 第4行数据
        CycleReportDataResponse.Row4Data row4 = new CycleReportDataResponse.Row4Data();
        row4.setLastCycleBlastTime(formatDateTime(lastCycleBlastTime));
        row4.setTheoreticalBlastTime(formatDateTime(theoreticalBlastTime));
        if (currentCycleBlastTime == null) {
            row4.setBlastOvertime("数据缺失");
        } else if (blastOvertimeMinutes != null) {
            row4.setBlastOvertime(formatDaysHoursMinutes(blastOvertimeMinutes));
        } else {
            row4.setBlastOvertime("");
        }
        row4.setCycleBlastDiffMinutes(cycleBlastDiffMinutes);
        row4.setCycleBlastDiffText(formatMinutesFromLong(cycleBlastDiffMinutes));
        response.setRow4(row4);
        
        // 第5行数据
        CycleReportDataResponse.Row5Data row5 = new CycleReportDataResponse.Row5Data();
        row5.setCycleStartTime(currentCycleBlastTime); // 循环实际响炮时间（装药爆破结束时间）
        row5.setPredictedNextBlastTimeByControl(formatDateTime(predictedNextByControl));
        row5.setPredictedNextBlastTimeByInterval(formatDateTime(predictedNextByInterval));
        response.setRow5(row5);
        
        // 工序列表数据
        List<Process> sortedProcesses = processes.stream()
                .sorted(Comparator.comparing(Process::getStartOrder, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
        
        List<CycleReportDataResponse.ProcessRowData> processList = new ArrayList<>();
        int totalActualMinutes = 0;
        int totalControlMinutes = 0;
        boolean hasActual = false;
        boolean hasControl = false;
        
        for (int i = 0; i < sortedProcesses.size(); i++) {
            Process process = sortedProcesses.get(i);
            CycleReportDataResponse.ProcessRowData processRow = new CycleReportDataResponse.ProcessRowData();
            
            // A列：工序名称
            processRow.setProcessName(process.getProcessName());
            
            // C、D列：开始时间
            LocalDateTime startTime = process.getActualStartTime() != null ? 
                    process.getActualStartTime() : process.getEstimatedStartTime();
            if (startTime != null) {
                processRow.setStartYearMonth(startTime.format(DateTimeFormatter.ofPattern("MM月dd日")));
                processRow.setStartTime(startTime.format(DateTimeFormatter.ofPattern("HH:mm")));
            }
            
            // E、F列：结束时间
            LocalDateTime endTime = process.getActualEndTime() != null ? 
                    process.getActualEndTime() : process.getEstimatedEndTime();
            if (endTime != null) {
                processRow.setEndYearMonth(endTime.format(DateTimeFormatter.ofPattern("MM月dd日")));
                processRow.setEndTime(endTime.format(DateTimeFormatter.ofPattern("HH:mm")));
            }
            
            // G、H列：耗时
            Integer actualMinutes = null;
            if (process.getActualStartTime() != null && process.getActualEndTime() != null) {
                actualMinutes = (int) Duration.between(process.getActualStartTime(), process.getActualEndTime()).toMinutes();
            } else if (process.getActualStartTime() != null) {
                actualMinutes = (int) Duration.between(process.getActualStartTime(), LocalDateTime.now()).toMinutes();
            }
            if (actualMinutes != null) {
                processRow.setActualMinutes(actualMinutes);
                processRow.setActualTimeText(formatMinutesStatic(actualMinutes));
                totalActualMinutes += actualMinutes;
                hasActual = true;
            }
            
            // I、J列：控制标准
            if (process.getControlTime() != null) {
                processRow.setControlTime(process.getControlTime());
                processRow.setControlTimeText(formatMinutesHourMinuteStatic(process.getControlTime()));
                totalControlMinutes += process.getControlTime();
                hasControl = true;
            }
            
            // K列：差值
            if (process.getControlTime() != null && actualMinutes != null) {
                int diffMinutes = process.getControlTime()- actualMinutes;
                processRow.setDiffText(formatMinutesWithSignStatic(diffMinutes));
                processRow.setOvertime(actualMinutes > process.getControlTime());
            } else {
                processRow.setOvertime(null);
            }
            
            // N列：情况说明（暂时为空）
            processRow.setDescription(process.getOvertimeReason());
            
            // O列：工序状态
            processRow.setStatus(getStatusDesc(process.getProcessStatus()));

            // 工序类别（来自工序字典的 category）
            processRow.setCategory(process.getCategory());
            
            processList.add(processRow);
        }
        response.setProcessList(processList);
        
        // 合计行数据
        CycleReportDataResponse.SummaryRowData summary = new CycleReportDataResponse.SummaryRowData();
        int totalDiffMinutes = totalActualMinutes - totalControlMinutes;
        summary.setTotalActualMinutes(hasActual ? totalActualMinutes : null);
        summary.setTotalActualTimeText(hasActual ? formatMinutesStatic(totalActualMinutes) : "");
        summary.setTotalControlMinutes(hasControl ? totalControlMinutes : null);
        summary.setTotalControlTimeText(hasControl ? formatMinutesHourMinuteStatic(totalControlMinutes) : "");
        summary.setTotalDiffText((hasActual || hasControl) ? formatMinutesWithSignStatic(totalDiffMinutes) : "");
        response.setSummary(summary);
        
        return response;
    }
    
    /**
     * 获取工序状态描述
     */
    private String getStatusDesc(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case "NOT_STARTED" -> "未开始";
            case "IN_PROGRESS" -> "进行中";
            case "COMPLETED" -> "已完成";
            default -> status;
        };
    }
    
    /**
     * 格式化分钟数为"X小时Y分钟"（静态方法）
     */
    private static String formatMinutesStatic(Integer minutes) {
        if (minutes == null) {
            return "";
        }
        int hrs = minutes / 60;
        int mins = minutes % 60;
        StringBuilder sb = new StringBuilder();
        if (hrs != 0) {
            sb.append(hrs).append("小时");
        }
        if (mins != 0) {
            sb.append(mins).append("分钟");
        }
        if (sb.length() == 0) {
            sb.append("0分钟");
        }
        return sb.toString();
    }
    
    /**
     * 格式化分钟数为"X小时Y分钟"（带符号，静态方法）
     */
    private static String formatMinutesWithSignStatic(Integer minutes) {
        if (minutes == null) {
            return "";
        }
        boolean negative = minutes < 0;
        String base = formatMinutesStatic(Math.abs(minutes));
        return negative ? "-" + base : base;
    }
    
    /**
     * 格式化分钟数为"X小时Y分钟"（静态方法，用于控制标准）
     */
    private static String formatMinutesHourMinuteStatic(Integer minutes) {
        if (minutes == null) {
            return "";
        }
        int hrs = minutes / 60;
        int mins = Math.abs(minutes % 60);
        return hrs + "小时" + mins + "分钟";
    }
    
    /**
     * 从 static 目录查找模板文件
     * 优先查找 classpath:static/模版.xlsx，如果不存在则查找第一个 .xlsx 文件
     */
    private File findTemplateFile() {
        try {
            // 优先查找 "模版.xlsx"
            Resource resource = resourceLoader.getResource("classpath:static/模版.xlsx");
            if (resource.exists()) {
                try {
                    return resource.getFile();
                } catch (IOException e) {
                    // 如果资源在 jar 包中，无法直接获取 File，需要复制到临时文件
                    log.debug("模板文件在jar包中，复制到临时文件: {}", e.getMessage());
                    return copyResourceToTempFile(resource);
                }
            }
            
            // 如果 "模版.xlsx" 不存在，查找 static 目录下的第一个 .xlsx 文件
            Resource staticDir = resourceLoader.getResource("classpath:static/");
            if (staticDir.exists()) {
                try {
                    File staticDirFile = staticDir.getFile();
                    if (staticDirFile.isDirectory()) {
                        File[] files = staticDirFile.listFiles((dir, name) -> 
                            name.toLowerCase().endsWith(".xlsx"));
                        if (files != null && files.length > 0) {
                            return files[0];
                        }
                    }
                } catch (IOException e) {
                    log.debug("无法直接访问static目录，尝试其他方式: {}", e.getMessage());
                }
            }
            
            log.warn("未找到报表模板文件（static目录下的xlsx）");
            return null;
        } catch (Exception e) {
            log.error("查找报表模板文件失败", e);
            return null;
        }
    }
    
    /**
     * 将资源复制到临时文件（用于处理jar包中的资源）
     */
    private File copyResourceToTempFile(Resource resource) throws IOException {
        try (java.io.InputStream inputStream = resource.getInputStream()) {
            File tempFile = File.createTempFile("excel_template_", ".xlsx");
            tempFile.deleteOnExit();
            try (java.io.FileOutputStream outputStream = new java.io.FileOutputStream(tempFile)) {
                inputStream.transferTo(outputStream);
            }
            log.debug("模板文件已复制到临时文件: {}", tempFile.getAbsolutePath());
            return tempFile;
        }
    }
    
    /**
     * 通过 EasyExcel 的 SheetWriteHandler 在模板创建后按坐标填充数据
     */
    private static class TemplateCellWriteHandler implements SheetWriteHandler {
        
        private final Map<String, TemplateCellValue> cellValues;
        private final List<Process> processes;
        
        TemplateCellWriteHandler(Map<String, TemplateCellValue> cellValues, List<Process> processes) {
            this.cellValues = cellValues;
            this.processes = processes;
        }
        
        @Override
        public void beforeSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
            // no-op
        }
        
        @Override
        public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
            Sheet sheet = writeSheetHolder.getSheet();
            if (sheet == null) {
                return;
            }
            
            // 填充单个单元格数据
            if (cellValues != null && !cellValues.isEmpty()) {
                cellValues.forEach((ref, val) -> applyValue(sheet, ref, val));
            }
            
            // 设置红色字体
            setRedFontCells(sheet);
            
            // 填充工序列表（从第10行开始，索引为9）
            if (processes != null && !processes.isEmpty()) {
                fillProcessList(sheet, processes);
            }
        }
        
        /**
         * 设置需要红色字体的单元格
         */
        private void setRedFontCells(Sheet sheet) {
            org.apache.poi.ss.usermodel.Workbook workbook = sheet.getWorkbook();
            org.apache.poi.ss.usermodel.Font redFont = workbook.createFont();
            redFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.RED.getIndex());
            
            org.apache.poi.ss.usermodel.CellStyle redStyle = workbook.createCellStyle();
            redStyle.setFont(redFont);
            
            // F2：循环结束时间（红色，值已在cellValues中设置）
            Row row2 = sheet.getRow(1); // 第2行，索引1
            if (row2 != null) {
                Cell cellF2 = row2.getCell(5); // F列，索引5
                if (cellF2 != null) {
                    // 保留原有样式，只设置红色字体
                    org.apache.poi.ss.usermodel.CellStyle style = workbook.createCellStyle();
                    if (cellF2.getCellStyle() != null) {
                        style.cloneStyleFrom(cellF2.getCellStyle());
                    }
                    style.setFont(redFont);
                    cellF2.setCellStyle(style);
                }
            }
            
            // K4、L4：响炮超时（合并单元格，红色，值已在cellValues中设置）
            Row row4 = sheet.getRow(3); // 第4行，索引3
            if (row4 != null) {
                Cell cellK4 = row4.getCell(10); // K列，索引10
                if (cellK4 != null) {
                    org.apache.poi.ss.usermodel.CellStyle style = workbook.createCellStyle();
                    if (cellK4.getCellStyle() != null) {
                        style.cloneStyleFrom(cellK4.getCellStyle());
                    }
                    style.setFont(redFont);
                    cellK4.setCellStyle(style);
                }
                Cell cellL4 = row4.getCell(11); // L列，索引11
                if (cellL4 != null) {
                    org.apache.poi.ss.usermodel.CellStyle style = workbook.createCellStyle();
                    if (cellL4.getCellStyle() != null) {
                        style.cloneStyleFrom(cellL4.getCellStyle());
                    }
                    style.setFont(redFont);
                    cellL4.setCellStyle(style);
                }
            }
            
            // C5：循环开始时间（红色）
            Row row5 = sheet.getRow(4); // 第5行，索引4
            if (row5 != null) {
                Cell cellC5 = row5.getCell(2); // C列，索引2
                if (cellC5 != null) {
                    org.apache.poi.ss.usermodel.CellStyle style = workbook.createCellStyle();
                    if (cellC5.getCellStyle() != null) {
                        style.cloneStyleFrom(cellC5.getCellStyle());
                    }
                    style.setFont(redFont);
                    cellC5.setCellStyle(style);
                }
            }
            
            // K5-P5：下循环响炮时间（合并单元格，红色）
            if (row5 != null) {
                for (int col = 10; col <= 15; col++) { // K到P列，索引10-15
                    Cell cell = row5.getCell(col);
                    if (cell != null) {
                        org.apache.poi.ss.usermodel.CellStyle style = workbook.createCellStyle();
                        if (cell.getCellStyle() != null) {
                            style.cloneStyleFrom(cell.getCellStyle());
                        }
                        style.setFont(redFont);
                        cell.setCellStyle(style);
                    }
                }
            }
        }
        
        /**
         * 填充工序列表到Excel表格中
         * 从第8行开始（索引7），列结构：
         * A=工序名称（如"1.扒渣"），C=开始年月，D=开始时分，E=结束年月，F=结束时分，
         * G=耗时（分钟），H=耗时（小时），I=控制标准（分钟），J=控制标准（小时），
         * K=控制标准和实际损耗差值（小时，-超时，+节时），N=情况说明，O=工序状态
         * 填充完所有工序后，将模板第12行（索引11）的内容复制到工序列表的下一行
         */
        private void fillProcessList(Sheet sheet, List<Process> processes) {
            int startRowIndex = 7; // 第8行（从0开始计数）
            int templateRowIndex = 11; // 模板第12行（从0开始计数）
            int templateProcessRowCount = 4; // 模板中工序行的数量（第8-11行，共4行）
            
            // 按开始顺序排序
            List<Process> sortedProcesses = processes.stream()
                    .sorted(Comparator.comparing(Process::getStartOrder, Comparator.nullsLast(Integer::compareTo)))
                    .collect(Collectors.toList());
            
            int totalActualMinutes = 0;
            int totalControlMinutes = 0;
            boolean hasActual = false;
            boolean hasControl = false;
            
            // 先保存模版第12行（"一个循环合计"）的内容和格式，因为填充工序数据时可能会覆盖它
            Row templateRow12 = sheet.getRow(templateRowIndex);
            Row savedTemplateRow12 = null;
            Map<Integer, Integer> savedColumnWidths = null; // 保存列宽信息
            if (templateRow12 != null) {
                // 创建一个临时sheet来保存模版第12行
                org.apache.poi.ss.usermodel.Workbook workbook = sheet.getWorkbook();
                Sheet tempSheet = workbook.createSheet("_temp_save_row12_");
                savedTemplateRow12 = tempSheet.createRow(0);
                copyRowContent(templateRow12, savedTemplateRow12);
                
                // 保存模版第12行相关列的列宽
                savedColumnWidths = new HashMap<>();
                int firstCellNum = templateRow12.getFirstCellNum();
                int lastCellNum = templateRow12.getLastCellNum();
                if (firstCellNum >= 0 && lastCellNum >= firstCellNum) {
                    for (int i = firstCellNum; i <= lastCellNum; i++) {
                        int columnWidth = sheet.getColumnWidth(i);
                        savedColumnWidths.put(i, columnWidth);
                    }
                }
            }
            
            // 获取模板中第一行工序的格式作为参考（第8行，索引7）
            Row templateFormatRow = sheet.getRow(startRowIndex);
            
            // 填充工序数据
            for (int i = 0; i < sortedProcesses.size(); i++) {
                Process process = sortedProcesses.get(i);
                int rowIndex = startRowIndex + i;
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    row = sheet.createRow(rowIndex);
                }
                
                // 如果填充到模版第12行（索引11），需要先清除该行的原有格式（灰色背景等）
                if (rowIndex == templateRowIndex) {
                    clearRowFormat(row);
                }
                
                // 如果超过模板中的行数，需要复制格式
                if (i >= templateProcessRowCount && templateFormatRow != null) {
                    copyRowFormat(templateFormatRow, row);
                }
                
                // 先为整行应用模板格式（确保所有单元格都有格式，包括空单元格）
                if (templateFormatRow != null) {
                    applyRowFormatFromTemplate(row, templateFormatRow);
                }
                
                // A列：工序名称（如"1.扒渣"）
                String processNameWithNumber = (i + 1) + "." + process.getProcessName();
                setCellValueWithFormat(row, 0, processNameWithNumber, templateFormatRow, 0);
                
                // C列：当前工序开始的月日
                LocalDateTime startTime = process.getActualStartTime() != null ? 
                        process.getActualStartTime() : process.getEstimatedStartTime();
                if (startTime != null) {
                    setCellValueWithFormat(row, 2, startTime.format(DateTimeFormatter.ofPattern("MM月dd日")), templateFormatRow, 2);
                } else {
                    // 即使值为空，也应用格式
                    applyCellFormat(row, 2, templateFormatRow, 2);
                }
                
                // D列：当前工序开始的小时和分钟
                if (startTime != null) {
                    setCellValueWithFormat(row, 3, startTime.format(DateTimeFormatter.ofPattern("HH:mm")), templateFormatRow, 3);
                } else {
                    applyCellFormat(row, 3, templateFormatRow, 3);
                }
                
                // E列：当前工序结束的月日
                LocalDateTime endTime = process.getActualEndTime() != null ? 
                        process.getActualEndTime() : process.getEstimatedEndTime();
                if (endTime != null) {
                    setCellValueWithFormat(row, 4, endTime.format(DateTimeFormatter.ofPattern("MM月dd日")), templateFormatRow, 4);
                } else {
                    applyCellFormat(row, 4, templateFormatRow, 4);
                }
                
                // F列：工序结束的小时和分钟
                if (endTime != null) {
                    setCellValueWithFormat(row, 5, endTime.format(DateTimeFormatter.ofPattern("HH:mm")), templateFormatRow, 5);
                } else {
                    applyCellFormat(row, 5, templateFormatRow, 5);
                }
                
                // G列：当前工序的耗时（分钟）
                Integer actualMinutes = null;
                if (process.getActualStartTime() != null && process.getActualEndTime() != null) {
                    actualMinutes = (int) Duration.between(process.getActualStartTime(), process.getActualEndTime()).toMinutes();
                } else if (process.getActualStartTime() != null) {
                    // 进行中的工序，计算已进行时间
                    actualMinutes = (int) Duration.between(process.getActualStartTime(), LocalDateTime.now()).toMinutes();
                }
                if (actualMinutes != null) {
                    totalActualMinutes += actualMinutes;
                    hasActual = true;
                }
                if (actualMinutes != null) {
                    setCellValueWithFormat(row, 6, actualMinutes, templateFormatRow, 6);
                } else {
                    applyCellFormat(row, 6, templateFormatRow, 6);
                }
                
                // H列：当前工序耗时（转成“X小时Y分钟”）
                if (actualMinutes != null) {
                    setCellValueWithFormat(row, 7, formatMinutes(actualMinutes), templateFormatRow, 7);
                } else {
                    applyCellFormat(row, 7, templateFormatRow, 7);
                }
                
                // I列：工序的控制标准（分钟），红色字体
                if (process.getControlTime() != null) {
                    setCellValueWithFormat(row, 8, process.getControlTime(), templateFormatRow, 8);
                    totalControlMinutes += process.getControlTime();
                    hasControl = true;
                } else {
                    applyCellFormat(row, 8, templateFormatRow, 8);
                }
                // 设置I列为红色字体
                Cell cellI = row.getCell(8);
                if (cellI != null) {
                    org.apache.poi.ss.usermodel.Workbook workbook = sheet.getWorkbook();
                    org.apache.poi.ss.usermodel.Font redFont = workbook.createFont();
                    redFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.RED.getIndex());
                    org.apache.poi.ss.usermodel.CellStyle redStyle = workbook.createCellStyle();
                    if (cellI.getCellStyle() != null) {
                        redStyle.cloneStyleFrom(cellI.getCellStyle());
                    }
                    redStyle.setFont(redFont);
                    cellI.setCellStyle(redStyle);
                }
                
                // J列：工序的控制标准（小时）
                if (process.getControlTime() != null) {
                    setCellValueWithFormat(row, 9, formatMinutesHourMinute(process.getControlTime()), templateFormatRow, 9);
                } else {
                    applyCellFormat(row, 9, templateFormatRow, 9);
                }
                
                // K列：当前工序控制标准和实际损耗的差值（显示为“X小时Y分钟”，负数表示节时）
                if (process.getControlTime() != null && actualMinutes != null) {
                    int diffMinutes = process.getControlTime()- actualMinutes ;
                    setCellValueWithFormat(row, 10, formatMinutesWithSign(diffMinutes), templateFormatRow, 10);
                } else {
                    applyCellFormat(row, 10, templateFormatRow, 10);
                }
                
                // N列：情况说明（超时原因）
                String overtimeReason = process.getOvertimeReason();
                if (overtimeReason != null && !overtimeReason.isBlank()) {
                    setCellValueWithFormat(row, 13, overtimeReason, templateFormatRow, 13);
                } else {
                    applyCellFormat(row, 13, templateFormatRow, 13);
                }
                
                // O列：工序状态
                String statusDesc = getStatusDesc(process.getProcessStatus());
                setCellValueWithFormat(row, 14, statusDesc, templateFormatRow, 14);
            }
            
            // 填充完所有工序后，将模版第12行（"一个循环合计"）复制到导出文件的最后一行，并保留原始格式
            if (savedTemplateRow12 != null) {
                // 计算导出文件的最后一行位置（工序列表之后的第一行）
                // 如果工序数量未超过模板自带的行数（4行），保持“一个循环合计”在模板原位置（第12行）
                int exportLastRowIndex = sortedProcesses.size() > templateProcessRowCount
                        ? startRowIndex + sortedProcesses.size()
                        : templateRowIndex;
                
                // 复制保存的模版第12行的内容和格式到导出文件的最后一行
                Row exportLastRow = sheet.getRow(exportLastRowIndex);
                if (exportLastRow == null) {
                    exportLastRow = sheet.createRow(exportLastRowIndex);
                }
                // 复制模版第12行的所有单元格内容、格式和样式（保留原来格式，包括灰色背景等）
                copyRowContent(savedTemplateRow12, exportLastRow);
                
                // 恢复列宽
                if (savedColumnWidths != null) {
                    for (Map.Entry<Integer, Integer> entry : savedColumnWidths.entrySet()) {
                        int columnIndex = entry.getKey();
                        int columnWidth = entry.getValue();
                        sheet.setColumnWidth(columnIndex, columnWidth);
                    }
                }
                
                // 填写“一个循环合计”行的汇总（G、H、I、J、K列）
                int totalDiffMinutes = totalActualMinutes - totalControlMinutes;
                
                setNumericIfPresent(exportLastRow, 6, hasActual ? totalActualMinutes : null);
                setStringIfPresent(exportLastRow, 7, hasActual ? formatMinutes(totalActualMinutes) : "");
                setNumericIfPresent(exportLastRow, 8, hasControl ? totalControlMinutes : null);
                setStringIfPresent(exportLastRow, 9, hasControl ? formatMinutesHourMinute(totalControlMinutes) : "");
                setStringIfPresent(exportLastRow, 10, (hasActual || hasControl) ? formatMinutesWithSign(totalDiffMinutes) : "");
                
                // 删除临时sheet
                int tempSheetIndex = sheet.getWorkbook().getSheetIndex("_temp_save_row12_");
                if (tempSheetIndex >= 0) {
                    sheet.getWorkbook().removeSheetAt(tempSheetIndex);
                }
            }
        }
        
        /**
         * 为单元格样式设置边框（提取公共方法，避免重复代码）
         */
        private void setBorders(org.apache.poi.ss.usermodel.CellStyle style) {
            org.apache.poi.ss.usermodel.IndexedColors borderColor = org.apache.poi.ss.usermodel.IndexedColors.BLACK;
            style.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            style.setTopBorderColor(borderColor.getIndex());
            style.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            style.setBottomBorderColor(borderColor.getIndex());
            style.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            style.setLeftBorderColor(borderColor.getIndex());
            style.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            style.setRightBorderColor(borderColor.getIndex());
        }
        
        /**
         * 创建并应用单元格样式（从模板复制样式并设置边框）
         */
        private org.apache.poi.ss.usermodel.CellStyle createCellStyleWithBorders(
                org.apache.poi.ss.usermodel.Workbook workbook, Cell templateCell) {
            org.apache.poi.ss.usermodel.CellStyle style = workbook.createCellStyle();
            if (templateCell != null && templateCell.getCellStyle() != null) {
                style.cloneStyleFrom(templateCell.getCellStyle());
            }
            setBorders(style);
            return style;
        }
        
        /**
         * 设置单元格值并应用格式（如果模板行存在）
         */
        private void setCellValueWithFormat(Row row, int colIndex, Object value, Row templateRow, int templateColIndex) {
            Cell cell = row.getCell(colIndex);
            if (cell == null) {
                cell = row.createCell(colIndex);
            }
            
            // 设置值
            if (value instanceof String) {
                cell.setCellValue((String) value);
            } else if (value instanceof Number) {
                cell.setCellValue(((Number) value).doubleValue());
            }
            
            // 如果模板行存在，复制对应列的格式（包括边框）
            if (templateRow != null) {
                Cell templateCell = templateRow.getCell(templateColIndex);
                org.apache.poi.ss.usermodel.Workbook workbook = row.getSheet().getWorkbook();
                cell.setCellStyle(createCellStyleWithBorders(workbook, templateCell));
            }
        }
        
        /**
         * 为单个单元格应用格式（即使值为空）
         */
        private void applyCellFormat(Row row, int colIndex, Row templateRow, int templateColIndex) {
            if (templateRow == null) {
                return;
            }
            Cell cell = row.getCell(colIndex);
            if (cell == null) {
                cell = row.createCell(colIndex);
            }
            Cell templateCell = templateRow.getCell(templateColIndex);
            org.apache.poi.ss.usermodel.Workbook workbook = row.getSheet().getWorkbook();
            cell.setCellStyle(createCellStyleWithBorders(workbook, templateCell));
        }
        
        /**
         * 为整行应用模板格式（确保所有需要的列都有格式）
         */
        private void applyRowFormatFromTemplate(Row row, Row templateRow) {
            if (templateRow == null) {
                return;
            }
            // 需要应用格式的列索引：A(0), B(1), C(2), D(3), E(4), F(5), G(6), H(7), I(8), J(9), K(10), L(11), N(13), O(14), P(15)
            int[] columnsToFormat = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
            for (int colIndex : columnsToFormat) {
                applyCellFormat(row, colIndex, templateRow, colIndex);
            }
        }
        
        /**
         * 复制行的格式（行高、单元格样式等）
         */
        private void copyRowFormat(Row sourceRow, Row targetRow) {
            if (sourceRow == null || targetRow == null) {
                return;
            }
            
            // 复制行高
            if (sourceRow.getHeight() > 0) {
                targetRow.setHeight(sourceRow.getHeight());
            }
            
            // 复制行样式（如果存在）
            if (sourceRow.getRowStyle() != null) {
                targetRow.setRowStyle(sourceRow.getRowStyle());
            }
        }
        
        /**
         * 清除行的格式（保留内容，清除样式）
         */
        private void clearRowFormat(Row row) {
            if (row == null) {
                return;
            }
            
            org.apache.poi.ss.usermodel.Workbook workbook = row.getSheet().getWorkbook();
            org.apache.poi.ss.usermodel.CellStyle defaultStyle = workbook.createCellStyle();
            
            // 遍历行的所有单元格，清除样式
            int firstCellNum = row.getFirstCellNum();
            int lastCellNum = row.getLastCellNum();
            if (firstCellNum >= 0 && lastCellNum >= firstCellNum) {
                for (int i = firstCellNum; i <= lastCellNum; i++) {
                    Cell cell = row.getCell(i);
                    if (cell != null) {
                        // 设置默认样式（无格式）
                        cell.setCellStyle(defaultStyle);
                    }
                }
            }
            
            // 清除行样式
            row.setRowStyle(defaultStyle);
            
            // 重置行高为默认值
            row.setHeight((short) -1);
        }
        
        /**
         * 复制源行的所有单元格内容到目标行
         */
        private void copyRowContent(Row sourceRow, Row targetRow) {
            if (sourceRow == null || targetRow == null) {
                return;
            }
            
            // 遍历源行的所有单元格
            for (int i = sourceRow.getFirstCellNum(); i <= sourceRow.getLastCellNum(); i++) {
                Cell sourceCell = sourceRow.getCell(i);
                if (sourceCell == null) {
                    continue;
                }
                
                Cell targetCell = targetRow.getCell(i);
                if (targetCell == null) {
                    targetCell = targetRow.createCell(i);
                }
                
                // 复制单元格值
                switch (sourceCell.getCellType()) {
                    case STRING -> targetCell.setCellValue(sourceCell.getStringCellValue());
                    case NUMERIC -> {
                        if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(sourceCell)) {
                            targetCell.setCellValue(sourceCell.getDateCellValue());
                        } else {
                            targetCell.setCellValue(sourceCell.getNumericCellValue());
                        }
                    }
                    case BOOLEAN -> targetCell.setCellValue(sourceCell.getBooleanCellValue());
                    case FORMULA -> targetCell.setCellFormula(sourceCell.getCellFormula());
                    case BLANK -> targetCell.setBlank();
                    default -> {
                        // 其他类型保持空白
                    }
                }
                
                // 复制单元格样式（如果存在）
                if (sourceCell.getCellStyle() != null) {
                    targetCell.setCellStyle(sourceCell.getCellStyle());
                }
            }
        }
        
        private String getStatusDesc(String status) {
            if (status == null) {
                return "";
            }
            return switch (status) {
                case "NOT_STARTED" -> "未开始";
                case "IN_PROGRESS" -> "进行中";
                case "COMPLETED" -> "已完成";
                default -> status;
            };
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
        
        private String formatMinutes(Integer minutes) {
            if (minutes == null) {
                return "";
            }
            int hrs = minutes / 60;
            int mins = minutes % 60;
            StringBuilder sb = new StringBuilder();
            if (hrs != 0) {
                sb.append(hrs).append("小时");
            }
            if (mins != 0) {
                sb.append(mins).append("分钟");
            }
            if (sb.length() == 0) {
                sb.append("0分钟");
            }
            return sb.toString();
        }
        
        private String formatMinutesWithSign(Integer minutes) {
            if (minutes == null) {
                return "";
            }
            boolean negative = minutes < 0;
            String base = formatMinutes(Math.abs(minutes));
            return negative ? "-" + base : base;
        }
        
        private String formatMinutesHourMinute(Integer minutes) {
            if (minutes == null) {
                return "";
            }
            int hrs = minutes / 60;
            int mins = Math.abs(minutes % 60);
            return hrs + "小时" + mins + "分钟";
        }
        
        private void setNumericIfPresent(Row row, int colIndex, Integer value) {
            if (value == null) {
                return;
            }
            Cell cell = row.getCell(colIndex);
            if (cell == null) {
                cell = row.createCell(colIndex);
            }
            cell.setCellValue(value);
        }
        
        private void setStringIfPresent(Row row, int colIndex, String value) {
            if (value == null) {
                return;
            }
            Cell cell = row.getCell(colIndex);
            if (cell == null) {
                cell = row.createCell(colIndex);
            }
            cell.setCellValue(value);
        }
    }
    
    private static class TemplateCellValue {
        enum Type {STRING, NUMBER, DATE}
        
        private final Type type;
        private final String stringValue;
        private final Double numberValue;
        private final Date dateValue;
        
        private TemplateCellValue(Type type, String stringValue, Double numberValue, Date dateValue) {
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
            Date date = Date.from(v.atZone(ZoneId.systemDefault()).toInstant());
            return new TemplateCellValue(Type.DATE, null, null, date);
        }
    }
    
    private static String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(DATE_TIME_FORMATTER);
    }
    
    /**
     * 将分钟数（Long）格式化为"X小时Y分钟"格式
     */
    private static String formatMinutesFromLong(Long minutes) {
        if (minutes == null) {
            return "";
        }
        long totalMinutes = Math.abs(minutes);
        long hrs = totalMinutes / 60;
        long mins = totalMinutes % 60;
        StringBuilder sb = new StringBuilder();
        if (hrs != 0) {
            sb.append(hrs).append("小时");
        }
        if (mins != 0) {
            sb.append(mins).append("分钟");
        }
        if (sb.length() == 0) {
            sb.append("0分钟");
        }
        return sb.toString();
    }
    
    /**
     * 将分钟数（Long）格式化为"X天X小时X分钟"格式
     */
    private static String formatDaysHoursMinutes(Long minutes) {
        if (minutes == null) {
            return "";
        }
        long totalMinutes = Math.abs(minutes);
        long days = totalMinutes / (24 * 60);
        long remainingMinutes = totalMinutes % (24 * 60);
        long hrs = remainingMinutes / 60;
        long mins = remainingMinutes % 60;
        StringBuilder sb = new StringBuilder();
        if (days != 0) {
            sb.append(days).append("天");
        }
        if (hrs != 0) {
            sb.append(hrs).append("小时");
        }
        if (mins != 0) {
            sb.append(mins).append("分钟");
        }
        if (sb.length() == 0) {
            sb.append("0分钟");
        }
        return sb.toString();
    }
    /**
     * 将相对里程数（BigDecimal）格式化为"DKX+Y.Z"格式
     *
     * @param relativeMileage 相对里程数
     * @return "DKX+Y.Z"格式的里程数
     */
    public static String formatMileage(BigDecimal relativeMileage) {
        if (relativeMileage == null) return "";

        // 计算绝对里程
        BigDecimal abs = PROJECT_START_MILEAGE.add(relativeMileage);

        double meters = abs.doubleValue();
        int km = (int) (meters / 1000);
        double rest = meters - km * 1000;

        return String.format("DK%d+%.1f", km, rest);
    }

    
    /**
     * 获取循环中"装药爆破"工序的结束时间（响炮时间）
     * 优先使用实际结束时间，其次使用预计结束时间
     * 
     * @param cycleId 循环ID
     * @return 装药爆破工序的结束时间，如果未找到该工序则返回null
     */
    private LocalDateTime getBlastTimeByCycleId(Long cycleId) {
        if (cycleId == null) {
            return null;
        }
        List<Process> processes = processService.getProcessesByCycleId(cycleId);
        for (Process process : processes) {
            // 查找工序名称为"装药爆破"的工序
            if ("装药爆破".equals(process.getProcessName())) {
                // 优先使用实际结束时间，其次使用预计结束时间
                if (process.getActualEndTime() != null) {
                    return process.getActualEndTime();
                } else if (process.getEstimatedEndTime() != null) {
                    return process.getEstimatedEndTime();
                }
            }
        }
        return null;
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
    
    @Override
    public TemplateControlDurationResponse getTemplateControlDuration(Long templateId) {
        log.info("获取模板控制时长，模板ID: {}", templateId);
        
        // 根据templateId获取模板工序（template_process表）
        TemplateProcess templateProcess = templateProcessMapper.selectById(templateId);
        if (templateProcess == null) {
            log.error("获取模板控制时长失败，模板工序不存在，模板ID: {}", templateId);
            throw new BusinessException(ResultCode.TEMPLATE_NOT_FOUND);
        }
        
        // 根据 template_id 查询模板信息
        Template template = templateMapper.selectById(templateProcess.getTemplateId());
        if (template == null) {
            log.error("获取模板控制时长失败，模板不存在，模板ID: {}", templateProcess.getTemplateId());
            throw new BusinessException(ResultCode.TEMPLATE_NOT_FOUND);
        }
        
        // 根据模板ID获取该模板下的所有工序模板
        List<TemplateProcess> templateProcesses = templateProcessMapper.selectList(new LambdaQueryWrapper<TemplateProcess>()
                .eq(TemplateProcess::getTemplateId, template.getId())
                .eq(TemplateProcess::getDeleted, 0)
                .orderByAsc(TemplateProcess::getDefaultOrder));
        
        if (templateProcesses.isEmpty()) {
            log.error("获取模板控制时长失败，模板没有工序定义，模板ID: {}, 模板名称: {}", 
                    template.getId(), template.getTemplateName());
            throw new BusinessException(ResultCode.TEMPLATE_NOT_FOUND);
        }
        
        // 计算控制时长：模板中所有工序的控制时间总和
        Integer controlDuration = templateProcesses.stream()
                .filter(tp -> tp.getControlTime() != null && tp.getControlTime() > 0)
                .mapToInt(TemplateProcess::getControlTime)
                .sum();
        
        log.info("计算模板控制时长，模板名称: {}, 工序数量: {}, 控制时长总和: {}分钟", 
                template.getTemplateName(), templateProcesses.size(), controlDuration);
        
        // 如果控制时长为0或null，设置为0（前端可以显示，但创建循环时会校验）
        if (controlDuration == null || controlDuration <= 0) {
            controlDuration = 0;
            log.warn("模板控制时长为0或无效，模板ID: {}, 模板名称: {}, 工序数量: {}", 
                    templateId, template.getTemplateName(), templateProcesses.size());
        }
        
        // 构建响应
        TemplateControlDurationResponse response = new TemplateControlDurationResponse();
        response.setTemplateId(templateId);
        response.setTemplateName(template.getTemplateName());
        response.setControlDuration(controlDuration);
        
        return response;
    }
}

