package com.zzw.zzwgx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzw.zzwgx.entity.UserProject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

/**
 * 用户项目关联Mapper
 */
@Mapper
public interface UserProjectMapper extends BaseMapper<UserProject> {
    
    /**
     * 幂等插入/恢复：若已存在则将 deleted 置为0，若不存在则插入
     */
    @Insert("""
        INSERT INTO sys_user_project (user_id, project_id, deleted, create_time, update_time)
        VALUES (#{userId}, #{projectId}, 0, NOW(), NOW())
        ON DUPLICATE KEY UPDATE deleted = 0, update_time = NOW()
        """)
    int upsertUserProject(@Param("userId") Long userId, @Param("projectId") Long projectId);
}

