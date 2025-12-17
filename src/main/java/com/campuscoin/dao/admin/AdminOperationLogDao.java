package com.campuscoin.dao.admin;

import com.campuscoin.model.admin.AdminOperationLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

/**
 * 管理员操作日志 DAO
 */
@Mapper
public interface AdminOperationLogDao {

    /**
     * 记录管理员操作日志
     */
    @Insert("INSERT INTO admin_operation_logs (admin_id, operation_type, target_type, target_id, operation_detail, reason, ip_address) " +
            "VALUES (#{adminId}, #{operationType}, #{targetType}, #{targetId}, #{operationDetail}, #{reason}, #{ipAddress})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AdminOperationLog log);
}
