package com.campuscoin.dao.admin;

import com.campuscoin.model.admin.AdminUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AdminUserDao {

    @Select("SELECT id, username, password_hash AS passwordHash, display_name AS displayName, enabled, last_login_at AS lastLoginAt, last_login_ip AS lastLoginIp, created_at AS createdAt, updated_at AS updatedAt FROM admin_users WHERE username = #{username}")
    AdminUser findByUsername(@Param("username") String username);

    @Update("UPDATE admin_users SET last_login_at = NOW(), last_login_ip = #{ip} WHERE id = #{id}")
    int updateLoginMeta(@Param("id") int id, @Param("ip") String ip);
}
