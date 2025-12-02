package com.zzw.zzwgx.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzw.zzwgx.entity.ProcessTemplate;
import com.zzw.zzwgx.mapper.ProcessTemplateMapper;
import com.zzw.zzwgx.service.ProcessTemplateService;
import org.springframework.stereotype.Service;

/**
 * 工序模板服务实现类
 */
@Service
public class ProcessTemplateServiceImpl extends ServiceImpl<ProcessTemplateMapper, ProcessTemplate> implements ProcessTemplateService {
}

