package com.zzw.zzwgx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzw.zzwgx.entity.Cycle;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 循环Mapper接口
 */
@Mapper
public interface CycleMapper extends BaseMapper<Cycle> {

    /**
     * 根据项目ID和循环号查询循环（包含逻辑删除的数据）
     * 用于在创建循环时，如果存在 deleted = 1 的记录，则直接恢复并复用
     */
    @Select("SELECT * FROM cycle WHERE project_id = #{projectId} AND cycle_number = #{cycleNumber} LIMIT 1")
    Cycle selectByProjectIdAndCycleNumberIncludeDeleted(@Param("projectId") Long projectId,
                                                       @Param("cycleNumber") Integer cycleNumber);

    /**
     * 恢复已逻辑删除的循环并覆盖关键业务字段（绕过 @TableLogic 的自动过滤）
     */
    @Update("""
        UPDATE cycle
        SET deleted = 0,
            control_duration = #{controlDuration},
            start_date = #{startDate},
            end_date = #{endDate},
            estimated_start_date = #{estimatedStartDate},
            estimated_end_date = #{estimatedEndDate},
            estimated_mileage = #{estimatedMileage},
            actual_mileage = #{actualMileage},
            development_method = #{developmentMethod},
            advance_length = #{advanceLength},
            rock_level = #{rockLevel},
            cycle_status = #{status},
            update_time = NOW()
        WHERE id = #{id}
        """)
    int restoreAndUpdateCycle(Cycle cycle);
    
    /**
     * 调整已逻辑删除的循环的 cycleNumber（绕过 @TableLogic 的自动过滤）
     * 用于释放被 deleted=1 的记录占用的 cycleNumber
     */
    @Update("UPDATE cycle SET cycle_number = #{newCycleNumber}, update_time = NOW() WHERE id = #{id}")
    int updateDeletedCycleNumber(@Param("id") Long id, @Param("newCycleNumber") Integer newCycleNumber);
    
    /**
     * 查询项目下所有循环（包括 deleted=1）的最大 cycleNumber
     * 用于为 deleted=1 的记录分配唯一的 cycleNumber
     */
    @Select("SELECT MAX(cycle_number) FROM cycle WHERE project_id = #{projectId}")
    Integer getMaxCycleNumberIncludeDeleted(@Param("projectId") Long projectId);
}

