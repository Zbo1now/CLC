package com.campuscoin.controller;

import com.campuscoin.model.DutyTask;
import com.campuscoin.model.DutyTaskSignup;
import com.campuscoin.payload.ApiResponse;
import com.campuscoin.service.DutyTaskService;
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
@RequestMapping("/api/duty-tasks")
public class DutyTaskController {

    private static final Logger logger = LoggerFactory.getLogger(DutyTaskController.class);

    private final DutyTaskService dutyTaskService;

    public DutyTaskController(DutyTaskService dutyTaskService) {
        this.dutyTaskService = dutyTaskService;
    }

    // 用户侧：值班任务列表（含状态、已报名状态、剩余名额）
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> list(HttpServletRequest request, HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamId == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        List<DutyTask> list = dutyTaskService.listForTeam(teamId);
        Map<String, Object> data = new HashMap<>();
        data.put("tasks", list);

        logger.info("查询值班任务列表: teamId={}, count={}", teamId, list != null ? list.size() : 0);
        return ResponseEntity.ok(ApiResponse.ok("获取成功", data));
    }

    // 用户侧：报名参加
    @PostMapping("/{taskId}/signups")
    public ResponseEntity<ApiResponse<Map<String, Object>>> signup(@PathVariable int taskId,
                                                                   HttpServletRequest request,
                                                                   HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamId == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        try {
            DutyTaskSignup signup = dutyTaskService.signup(teamId, taskId);
            Map<String, Object> data = new HashMap<>();
            data.put("signup", signup);
            return ResponseEntity.ok(ApiResponse.ok("报名成功", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            logger.error("值班任务报名异常: teamId={}, taskId={}", teamId, taskId, e);
            return ResponseEntity.status(500).body(ApiResponse.fail("报名失败，请稍后重试"));
        }
    }
}
