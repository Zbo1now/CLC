package com.campuscoin.controller;

import com.campuscoin.model.Checkin;
import com.campuscoin.model.Team;
import com.campuscoin.payload.ApiResponse;
import com.campuscoin.service.BaiduService;
import com.campuscoin.service.TransactionService;
import com.campuscoin.dao.CheckinDao;
import com.campuscoin.dao.TeamDao;
import com.campuscoin.util.ConfigCache;
import com.campuscoin.util.SessionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/face")
public class CheckinController {

    private static final Logger logger = LoggerFactory.getLogger(CheckinController.class);
    private final BaiduService baiduService;
    private final TeamDao teamDao;
    private final CheckinDao checkinDao;
    private final TransactionService transactionService;
    private final ConfigCache configCache;

    public CheckinController(BaiduService baiduService,
                             TeamDao teamDao,
                             CheckinDao checkinDao,
                             TransactionService transactionService,
                             ConfigCache configCache) {
        this.baiduService = baiduService;
        this.teamDao = teamDao;
        this.checkinDao = checkinDao;
        this.transactionService = transactionService;
        this.configCache = configCache;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> registerFace(@RequestBody FaceRequest req,
                                                        HttpServletRequest request,
                                                        HttpSession session) {
        String teamName = SessionHelper.resolveTeamName(request, session);
        if (teamName == null) {
            logger.warn("人脸录入失败: session/teamName 缺失");
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        logger.info("尝试为人脸录入: {}", teamName);
        
        // 修改：仅上传到 BOS，不再注册到百度人脸库 (FaceSet)
        // 1. 上传到 BOS 并保存 URL 到数据库（作为基准照片记录）
        String fileName = "faces/" + teamName + "_" + System.currentTimeMillis() + ".jpg";
        String bosUrl = baiduService.uploadToBos(req.getImage(), fileName);
        logger.info("BOS upload result: team={}, file={}, url={}", teamName, fileName, bosUrl);
        
        if (bosUrl != null) {
            int updated = teamDao.updateFaceImage(teamName, bosUrl);
            if (updated > 0) {
                logger.info("人脸录入成功 (BOS -> DB): {} -> {}", teamName, bosUrl);
                return ResponseEntity.ok(ApiResponse.ok("人脸录入成功", null));
            }
            logger.warn("人脸录入: 图片上传成功但数据库更新失败: {}", teamName);
            return ResponseEntity.status(500).body(ApiResponse.fail("人脸录入失败，请稍后重试"));
        } else {
            logger.error("人脸录入失败: BOS上传错误");
            return ResponseEntity.badRequest().body(ApiResponse.fail("人脸录入失败，请重试"));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<Checkin>>> getCheckinHistory(@RequestParam int year,
                                                                       @RequestParam int month,
                                                                       HttpServletRequest request,
                                                                       HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamId == null) {
            logger.warn("获取打卡历史失败: session/teamId 缺失");
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }
        logger.info("获取打卡历史: teamId={}, year={}, month={}", teamId, year, month);
        List<Checkin> history = checkinDao.findByTeamAndMonth(teamId, year, month);
        return ResponseEntity.ok(ApiResponse.ok("获取成功", history));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus(HttpServletRequest request, HttpSession session) {
        String teamName = SessionHelper.resolveTeamName(request, session);
        if (teamName == null) {
            logger.warn("获取打卡状态失败: session/teamName 缺失");
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }
        Team team = teamDao.findByName(teamName);
        if (team != null) {
            String faceImage = team.getFaceImage();
            boolean hasFaceInfo = faceImage != null && !faceImage.isEmpty();
            logger.info("查询人脸状态: {}，人脸字段 {}", teamName, hasFaceInfo ? "已录入" : "未录入");
            Map<String, Object> status = new HashMap<>();
            status.put("hasFace", hasFaceInfo);
            status.put("currentStreak", team.getCurrentStreak());
            status.put("balance", team.getBalance());
            status.put("faceImage", faceImage);
            logger.info("获取打卡状态成功: {}", teamName);
            return ResponseEntity.ok(ApiResponse.ok("获取成功", status));
        }
        logger.warn("获取打卡状态失败: 用户不存在 {}", teamName);
        return ResponseEntity.badRequest().body(ApiResponse.fail("用户不存在"));
    }

    @PostMapping("/checkin")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkIn(@RequestBody FaceRequest req,
                                                                    HttpServletRequest request,
                                                                    HttpSession session) {
        String teamName = SessionHelper.resolveTeamName(request, session);
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamName == null || teamId == null) {
            logger.warn("打卡失败: session/team 缺失");
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }
        
        logger.info("尝试打卡: {}", teamName);

        // 检查今日是否已打卡
        LocalDate today = LocalDate.now();
        Date sqlDate = Date.valueOf(today);
        if (checkinDao.hasCheckedInToday(teamId, sqlDate)) {
            logger.info("打卡被拒绝: 今日已打卡 {}", teamName);
            return ResponseEntity.badRequest().body(ApiResponse.fail("今日已打卡，请明天再来"));
        }

        // 获取当前用户信息以获取基准人脸照片
        Team team = teamDao.findByName(teamName);
        if (team == null) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("用户不存在"));
        }
        String registeredFaceUrl = team.getFaceImage();

        if (registeredFaceUrl == null || registeredFaceUrl.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("请先进行人脸录入"));
        }

        // 修改：使用 1:1 人脸比对 (Match) 而不是 1:N 搜索 (Search)
        boolean isMatch = baiduService.matchFace(req.getImage(), registeredFaceUrl);
        logger.info("人脸比对结果: {}", isMatch ? "匹配" : "不匹配");
        
        if (isMatch) {
            int streak = team.getCurrentStreak();
            Date lastCheckin = team.getLastCheckinDate();
            
            // 计算连续打卡
            if (lastCheckin != null && lastCheckin.toLocalDate().plusDays(1).equals(today)) {
                streak++;
            } else {
                streak = 1;
            }
            
            // 计算奖励（基础奖励可由系统配置控制）
            int baseReward = configCache.getInt("reward.checkin", 10);
            int bonus = (streak > 3) ? 20 : 0;
            int totalReward = baseReward + bonus;
            
            // 更新数据库
            checkinDao.create(new Checkin(teamId, sqlDate, totalReward));
            teamDao.updateCheckinStatus(teamId, team.getBalance() + totalReward, streak, sqlDate);

            // 记录流水：正向入账
            try {
                String desc = bonus > 0 ? ("日常打卡 + 连续奖励") : "日常打卡";
                transactionService.record(teamId, "CHECKIN", totalReward, desc);
            } catch (Exception e) {
                // 流水失败不影响打卡主流程，但需要有日志便于排查
                logger.error("打卡流水记录失败: teamId={}, reward={}", teamId, totalReward, e);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("coinsEarned", totalReward);
            result.put("streak", streak);
            result.put("message", "打卡成功！获得 " + totalReward + " 币" + (bonus > 0 ? " (含连续打卡奖励)" : ""));
            
            logger.info("打卡成功: {}, 奖励={}, 连续天数={}", teamName, totalReward, streak);
            return ResponseEntity.ok(ApiResponse.ok("打卡成功", result));
        }
        
        logger.warn("打卡失败: 人脸不匹配 {}", teamName);
        return ResponseEntity.badRequest().body(ApiResponse.fail("打卡失败：人脸不匹配"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> faceLogin(@RequestBody FaceRequest req, HttpSession session) {
        logger.info("尝试刷脸登录");
        String userId = baiduService.searchFace(req.getImage());
        logger.info("刷脸登录搜索结果: {}", userId);
        
        if (userId != null) {
            Team team = teamDao.findByName(userId);
            if (team != null) {
                session.setAttribute("teamId", team.getId());
                session.setAttribute("teamName", team.getTeamName());
                
                Map<String, Object> data = new HashMap<>();
                data.put("teamName", team.getTeamName());
                data.put("balance", team.getBalance());
                
                logger.info("刷脸登录成功: {}", team.getTeamName());
                return ResponseEntity.ok(ApiResponse.ok("人脸识别成功", data));
            }
        }
        
        logger.warn("刷脸登录失败: 未找到匹配用户");
        return ResponseEntity.badRequest().body(ApiResponse.fail("人脸识别失败，未找到匹配用户"));
    }

    public static class FaceRequest {
        private String teamName;
        private String image;

        public String getTeamName() { return teamName; }
        public void setTeamName(String teamName) { this.teamName = teamName; }
        public String getImage() { return image; }
        public void setImage(String image) { this.image = image; }
    }
}
