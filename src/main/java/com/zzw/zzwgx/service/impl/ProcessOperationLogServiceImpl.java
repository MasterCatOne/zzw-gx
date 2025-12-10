package com.zzw.zzwgx.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzw.zzwgx.entity.ProcessOperationLog;
import com.zzw.zzwgx.mapper.ProcessOperationLogMapper;
import com.zzw.zzwgx.service.ProcessOperationLogService;
import org.springframework.stereotype.Service;

@Service
public class ProcessOperationLogServiceImpl extends ServiceImpl<ProcessOperationLogMapper, ProcessOperationLog>
        implements ProcessOperationLogService {
}

