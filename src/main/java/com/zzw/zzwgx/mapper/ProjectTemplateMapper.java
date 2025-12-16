package com.zzw.zzwgx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzw.zzwgx.entity.ProjectTemplate;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 工点-模板关联Mapper
 */
@Mapper
public interface ProjectTemplateMapper extends BaseMapper<ProjectTemplate> {

    /**
     * 幂等插入/恢复：存在则将 deleted 置为0，不存在则插入
     */
    @Insert("""
        INSERT INTO project_template (project_id, template_id, deleted, create_time, update_time)
        VALUES (#{projectId}, #{templateId}, 0, NOW(), NOW())
        ON DUPLICATE KEY UPDATE deleted = 0, update_time = NOW()
        """)
    int upsertProjectTemplate(@Param("projectId") Long projectId, @Param("templateId") Long templateId);
}

