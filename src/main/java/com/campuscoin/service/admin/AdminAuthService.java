package com.campuscoin.service.admin;

import com.campuscoin.dao.admin.AdminUserDao;
import com.campuscoin.model.admin.AdminUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {

    private static final Logger logger = LoggerFactory.getLogger(AdminAuthService.class);

    private final AdminUserDao adminUserDao;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminAuthService(AdminUserDao adminUserDao) {
        this.adminUserDao = adminUserDao;
    }

    public AdminUser login(String username, String password, String ip) {
        AdminUser user = adminUserDao.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (user.getEnabled() == null || !user.getEnabled()) {
            throw new IllegalStateException("账号已被禁用");
        }
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        try {
            adminUserDao.updateLoginMeta(user.getId(), ip);
        } catch (Exception ex) {
            logger.warn("更新管理员登录信息失败: username={}, ip={}", username, ip, ex);
        }

        return user;
    }
}
