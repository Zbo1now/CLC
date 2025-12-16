package com.campuscoin.service;

import com.campuscoin.dao.TeamDao;
import com.campuscoin.model.Team;
import com.campuscoin.util.LogUtil;
import com.campuscoin.util.PasswordUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.logging.Logger;

@Service
public class AuthService {
    private static final Logger logger = LogUtil.getLogger(AuthService.class);
    private static final int MIN_PASSWORD_LEN = 6;
    private static final int INITIAL_COINS = 500;

    private final TeamDao teamDao;

    public AuthService(TeamDao teamDao) {
        this.teamDao = teamDao;
    }

    @Transactional
    public Team register(String teamName, String password, String contactName, String contactPhone) {
        logger.info("尝试注册团队: " + teamName);
        if (teamName == null || teamName.trim().isEmpty()) {
            logger.warning("注册失败: 团队名称为空");
            throw new IllegalArgumentException("团队名称不能为空");
        }
        if (password == null || password.length() < MIN_PASSWORD_LEN) {
            logger.warning("注册失败: 密码长度不足");
            throw new IllegalArgumentException("密码长度至少为6位");
        }
        if (contactName == null || contactName.trim().isEmpty()) {
            logger.warning("注册失败: 联系人为空");
            throw new IllegalArgumentException("联系人姓名不能为空");
        }
        if (contactPhone == null || contactPhone.trim().isEmpty()) {
            logger.warning("注册失败: 联系电话为空");
            throw new IllegalArgumentException("联系电话不能为空");
        }
        Optional<Team> exists = teamDao.findByName(teamName.trim());
        if (exists.isPresent()) {
            logger.warning("注册失败: 团队名称已存在 - " + teamName);
            throw new IllegalStateException("团队名称已存在");
        }
        String hash = PasswordUtil.hashPassword(password);
        Team team = new Team();
        team.setTeamName(teamName.trim());
        team.setPasswordHash(hash);
        team.setContactName(contactName.trim());
        team.setContactPhone(contactPhone.trim());
        team.setBalance(INITIAL_COINS);
        boolean ok = teamDao.createTeam(team);
        if (!ok) {
            logger.severe("注册失败: 数据库插入失败 - " + teamName);
            throw new IllegalStateException("注册团队失败，请稍后重试");
        }
        logger.info("团队注册成功: " + teamName);
        return teamDao.findByName(teamName.trim()).orElse(team);
    }

    public Team login(String teamName, String password) {
        logger.info("尝试登录: " + teamName);
        if (teamName == null || password == null) {
            logger.warning("登录失败: 参数缺失");
            throw new IllegalArgumentException("团队名称和密码不能为空");
        }
        Optional<Team> opt = teamDao.findByName(teamName.trim());
        if (!opt.isPresent()) {
            logger.warning("登录失败: 用户不存在 - " + teamName);
            throw new IllegalArgumentException("登录失败：用户不存在");
        }
        Team team = opt.get();
        boolean valid = PasswordUtil.verifyPassword(password, team.getPasswordHash());
        if (!valid) {
            logger.warning("登录失败: 密码错误 - " + teamName);
            throw new IllegalArgumentException("登录失败：密码错误");
        }
        logger.info("登录成功: " + teamName);
        return team;
    }
}
