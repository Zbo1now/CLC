package com.campuscoin.controller;

import com.campuscoin.dao.TeamDao;
import com.campuscoin.dao.TransactionDao;
import com.campuscoin.model.Team;
import com.campuscoin.model.Transaction;
import com.campuscoin.payload.ApiResponse;
import com.campuscoin.util.SessionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private static final Logger logger = LoggerFactory.getLogger(TeamController.class);

    private final TeamDao teamDao;
    private final TransactionDao transactionDao;

    public TeamController(TeamDao teamDao, TransactionDao transactionDao) {
        this.teamDao = teamDao;
        this.transactionDao = transactionDao;
    }

    // 首页用：查询当前团队余额 + 最近流水
    @GetMapping("/me/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> mySummary(HttpServletRequest request, HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        String teamName = SessionHelper.resolveTeamName(request, session);
        if (teamId == null || teamName == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        Team team = teamDao.findByName(teamName);
        if (team == null) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("用户不存在"));
        }

        List<Transaction> recent = transactionDao.listRecentByTeam(teamId, 10);

        Map<String, Object> data = new HashMap<>();
        data.put("teamName", team.getTeamName());
        data.put("balance", team.getBalance());
        data.put("transactions", recent);

        logger.info("查询团队摘要: teamId={}, balance={}, txns={}", teamId, team.getBalance(), recent.size());
        return ResponseEntity.ok(ApiResponse.ok("获取成功", data));
    }
}
