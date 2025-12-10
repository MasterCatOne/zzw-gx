package com.zzw.zzwgx.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 记录施工人员对工序的操作（开始、完成、提交超时原因等）
 */
@Data
@TableName("process_operation_log")
public class ProcessOperationLog {

    @TableId
    private Long id;

    /** 工序ID */
    private Long processId;

    /** 操作人ID */
    private Long userId;

    /** 操作类型：START/FINISH/FINISH_AND_NEXT/OVERTIME_REASON/CREATE_AND_START 等 */
    private String action;

    /** 备注信息，如超时原因或提示信息 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;
}

