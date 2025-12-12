package com.zzw.zzwgx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzw.zzwgx.entity.UserRoleRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户角色关联Mapper
 */
@Mapper
public interface UserRoleRelationMapper extends BaseMapper<UserRoleRelation> {
    
    /**
     * 恢复已删除的用户-角色关联（直接使用 SQL 更新，绕过逻辑删除）
     */
    @Update("UPDATE sys_user_role SET deleted = 0, update_time = NOW() WHERE user_id = #{userId} AND role_id = #{roleId} AND deleted = 1")
    int restoreDeletedRelation(@Param("userId") Long userId, @Param("roleId") Long roleId);
}

