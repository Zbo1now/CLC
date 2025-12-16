package com.campuscoin.service;

import com.campuscoin.dao.TeamDao;
import com.campuscoin.model.Team;
import com.campuscoin.service.TransactionService;
import com.campuscoin.util.LogUtil;
import com.campuscoin.util.PasswordUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.logging.Logger;

@Service
public class AuthService {
    private static final Logger logger = LogUtil.getLogger(AuthService.class);
    private static final int MIN_PASSWORD_LEN = 6;
    private static final int INITIAL_COINS = 500;

    private final TeamDao teamDao;
    private final TransactionService transactionService;

    public AuthService(TeamDao teamDao, TransactionService transactionService) {
        this.teamDao = teamDao;
        this.transactionService = transactionService;
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
        Team exists = teamDao.findByName(teamName.trim());
        if (exists != null) {
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
        int rows = teamDao.createTeam(team);
        if (rows != 1) {
            logger.severe("注册失败: 数据库插入失败 - " + teamName);
            throw new IllegalStateException("注册团队失败，请稍后重试");
        }

        // 注册赠送虚拟币：记录一条入账流水，供首页展示
        try {
            int teamId = team.getId();
            if (teamId <= 0) {
                Team tmp = teamDao.findByName(teamName.trim());
                if (tmp != null) {
                    teamId = tmp.getId();
                }
            }
            if (teamId > 0) {
                transactionService.record(teamId, "REGISTER_BONUS", INITIAL_COINS, "注册赠送");
            } else {
                logger.warning("注册奖励流水未写入: teamId 未获取到 teamName=" + teamName);
            }
        } catch (Exception e) {
            logger.warning("注册奖励流水写入失败: " + e.getMessage());
        }

        logger.info("团队注册成功: " + teamName);
        Team fresh = teamDao.findByName(teamName.trim());
        return fresh != null ? fresh : team;
    }

    public Team login(String teamName, String password) {
        logger.info("尝试登录: " + teamName);
        if (teamName == null || password == null) {
            logger.warning("登录失败: 参数缺失");
            throw new IllegalArgumentException("团队名称和密码不能为空");
        }
        Team team = teamDao.findByName(teamName.trim());
        if (team == null) {
            logger.warning("登录失败: 用户不存在 - " + teamName);
            throw new IllegalArgumentException("登录失败：用户不存在");
        }
        String storedHash = team.getPasswordHash();
        if (storedHash == null || storedHash.isEmpty()) {
            logger.severe("登录失败: 密码 hash 缺失 - " + teamName);
            throw new IllegalArgumentException("登录失败：账户未设置密码，请联系管理员");
        }
        boolean valid = PasswordUtil.verifyPassword(password, storedHash);
        if (!valid) {
            logger.warning("登录失败: 密码错误 - " + teamName);
            throw new IllegalArgumentException("登录失败：密码错误");
        }
        logger.info("登录成功: " + teamName);
        return team;
    }
}
