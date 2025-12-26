package com.campuscoin.controller;

import com.campuscoin.model.TrainingEvent;
import com.campuscoin.model.TrainingParticipation;
import com.campuscoin.model.TrainingParticipationView;
import com.campuscoin.payload.ApiResponse;
import com.campuscoin.service.TrainingService;
import com.campuscoin.util.SessionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/training-events")
public class TrainingEventController {

    private static final Logger logger = LoggerFactory.getLogger(TrainingEventController.class);

    private final TrainingService trainingService;

    public TrainingEventController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    // 用户侧：活动列表（含活动状态 + 我的申报状态）
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> list(HttpServletRequest request, HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamId == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        List<TrainingEvent> list = trainingService.listForTeam(teamId);
        Map<String, Object> data = new HashMap<>();
        data.put("events", list);
        logger.info("查询培训活动列表: teamId={}, count={}", teamId, list != null ? list.size() : 0);
        return ResponseEntity.ok(ApiResponse.ok("获取成功", data));
    }

    // 用户侧：活动详情（含活动状态 + 我的申报状态）
    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detail(@PathVariable int eventId,
                                                                   HttpServletRequest request,
                                                                   HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamId == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        TrainingEvent e = trainingService.getEventForTeam(teamId, eventId);
        if (e == null) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("活动不存在"));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("event", e);
        return ResponseEntity.ok(ApiResponse.ok("获取成功", data));
    }

    // 用户侧：提交参与证明
    @PostMapping("/{eventId}/participations")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submit(@PathVariable int eventId,
                                                                   @RequestBody(required = false) SubmitRequest req,
                                                                   HttpServletRequest request,
                                                                   HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        String teamName = SessionHelper.resolveTeamName(request, session);
        if (teamId == null || teamName == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        try {
            TrainingParticipation p = trainingService.submitParticipation(
                    teamId,
                    eventId,
                    req != null ? req.getProofBase64() : null,
                    req != null ? req.getNote() : null
            );
            Map<String, Object> data = new HashMap<>();
            data.put("participation", p);
            logger.info("培训申报提交API成功: teamId={}, teamName={}, eventId={}, participationId={}",
                    teamId, teamName, eventId, p != null ? p.getId() : null);
            return ResponseEntity.ok(ApiResponse.ok("提交成功，等待审核", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            logger.error("培训申报提交异常: teamId={}, teamName={}, eventId={}", teamId, teamName, eventId, e);
            return ResponseEntity.status(500).body(ApiResponse.fail("提交失败，请稍后重试"));
        }
    }

    // 用户侧：报名参加
    @PostMapping("/{eventId}/signups")
    public ResponseEntity<ApiResponse<Map<String, Object>>> signup(@PathVariable int eventId,
                                                                   HttpServletRequest request,
                                                                   HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        String teamName = SessionHelper.resolveTeamName(request, session);
        if (teamId == null || teamName == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        try {
            TrainingParticipation p = trainingService.signup(teamId, eventId);
            Map<String, Object> data = new HashMap<>();
            data.put("participation", p);
            logger.info("培训活动报名成功: teamId={}, teamName={}, eventId={}, participationId={}",
                    teamId, teamName, eventId, p != null ? p.getId() : null);
            return ResponseEntity.ok(ApiResponse.ok("报名成功", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            logger.error("培训活动报名异常: teamId={}, teamName={}, eventId={}", teamId, teamName, eventId, e);
            return ResponseEntity.status(500).body(ApiResponse.fail("报名失败，请稍后重试"));
        }
    }

    // 用户侧：我的申报列表
    @GetMapping("/me/participations")
    public ResponseEntity<ApiResponse<List<TrainingParticipationView>>> myParticipations(HttpServletRequest request,
                                                                                        HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamId == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        List<TrainingParticipationView> list = trainingService.listMyParticipations(teamId);
        logger.info("查询我的培训申报: teamId={}, count={}", teamId, list != null ? list.size() : 0);
        return ResponseEntity.ok(ApiResponse.ok("获取成功", list));
    }

    public static class SubmitRequest {
        private String proofBase64;
        private String note;

        public String getProofBase64() {
            return proofBase64;
        }

        public void setProofBase64(String proofBase64) {
            this.proofBase64 = proofBase64;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }
}
