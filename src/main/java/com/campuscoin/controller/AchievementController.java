package com.campuscoin.controller;

import com.campuscoin.model.AchievementSubmission;
import com.campuscoin.payload.ApiResponse;
import com.campuscoin.service.AchievementService;
import com.campuscoin.util.SessionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/achievements")
public class AchievementController {

    private static final Logger logger = LoggerFactory.getLogger(AchievementController.class);

    private final AchievementService achievementService;

    public AchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submit(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "subType", required = false) String subType,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestPart(value = "proof", required = false) MultipartFile proof,
            HttpServletRequest request,
            HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        String teamName = SessionHelper.resolveTeamName(request, session);
        if (teamId == null || teamName == null) {
            logger.warn("成果提交失败: 未登录");
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }
        try {
            AchievementSubmission created;
            if (proof != null && proof.getSize() > 0) {
                created = achievementService.submitMultipart(teamId, category, subType, title, description, proof);
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.fail("请上传证明材料"));
            }
            Map<String, Object> data = new HashMap<>();
            data.put("submission", created);
            logger.info("成果提交API成功: teamId={}, teamName={}, id={}", teamId, teamName, created != null ? created.getId() : null);
            return ResponseEntity.ok(ApiResponse.ok("提交成功，等待审核", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            logger.error("成果提交异常: teamId={}, teamName={}", teamId, teamName, e);
            return ResponseEntity.status(500).body(ApiResponse.fail("提交失败，请稍后重试"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<AchievementSubmission>>> myList(HttpServletRequest request, HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamId == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        List<AchievementSubmission> list = achievementService.listMine(teamId);
        logger.info("查询我的成果列表: teamId={}, count={}", teamId, list != null ? list.size() : 0);
        return ResponseEntity.ok(ApiResponse.ok("获取成功", list));
    }

    @GetMapping("/me/{id}")
    public ResponseEntity<ApiResponse<AchievementSubmission>> myDetail(@PathVariable int id,
                                                                       HttpServletRequest request,
                                                                       HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamId == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        AchievementSubmission sub = achievementService.getMine(teamId, id);
        if (sub == null) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("成果不存在"));
        }
        return ResponseEntity.ok(ApiResponse.ok("获取成功", sub));
    }
}
