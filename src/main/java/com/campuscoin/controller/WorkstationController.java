package com.campuscoin.controller;

import com.campuscoin.model.Workstation;
import com.campuscoin.model.WorkstationLease;
import com.campuscoin.payload.ApiResponse;
import com.campuscoin.service.WorkstationService;
import com.campuscoin.dao.WorkstationDao;
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
@RequestMapping("/api/stations")
public class WorkstationController {

    private static final Logger logger = LoggerFactory.getLogger(WorkstationController.class);

    private final WorkstationService workstationService;
    private final WorkstationDao workstationDao;

    public WorkstationController(WorkstationService workstationService, WorkstationDao workstationDao) {
        this.workstationService = workstationService;
        this.workstationDao = workstationDao;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Workstation>>> listStations(HttpServletRequest request, HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        logger.info("查询工位列表: teamId={}", teamId);
        List<Workstation> list = workstationService.listWorkstationsForCurrentMonth();
        return ResponseEntity.ok(ApiResponse.ok("获取成功", list));
    }

    @GetMapping("/me/lease")
    public ResponseEntity<ApiResponse<Map<String, Object>>> myLease(HttpServletRequest request, HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamId == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        logger.info("查询我的租约: teamId={}", teamId);
        WorkstationLease lease = workstationService.getMyActiveLease(teamId);
        Map<String, Object> data = new HashMap<>();
        data.put("lease", lease);

        if (lease != null) {
            Workstation ws = workstationDao.findById(lease.getWorkstationId());
            data.put("workstation", ws);
        } else {
            data.put("workstation", null);
        }
        return ResponseEntity.ok(ApiResponse.ok("获取成功", data));
    }

    @PostMapping("/{stationId}/lease")
    public ResponseEntity<ApiResponse<Map<String, Object>>> lease(@PathVariable int stationId,
                                                                 @RequestBody(required = false) LeaseRequest req,
                                                                 HttpServletRequest request,
                                                                 HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamId == null) {
            logger.warn("工位租赁失败: 未登录 stationId={}", stationId);
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        int months = (req != null && req.getMonths() != null) ? req.getMonths() : 1;
        try {
            WorkstationLease lease = workstationService.rent(teamId, stationId, months);
            Map<String, Object> data = new HashMap<>();
            data.put("lease", lease);
            return ResponseEntity.ok(ApiResponse.ok("租赁成功", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            logger.error("工位租赁异常: teamId={}, stationId={}", teamId, stationId, e);
            return ResponseEntity.status(500).body(ApiResponse.fail("租赁失败，请稍后重试"));
        }
    }

    @PostMapping("/me/lease/renew")
    public ResponseEntity<ApiResponse<Map<String, Object>>> renew(@RequestBody(required = false) LeaseRequest req,
                                                                 HttpServletRequest request,
                                                                 HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamId == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        int months = (req != null && req.getMonths() != null) ? req.getMonths() : 1;
        try {
            logger.info("工位续租请求: teamId={}, months={}", teamId, months);
            WorkstationLease lease = workstationService.renewMyLease(teamId, months);
            Map<String, Object> data = new HashMap<>();
            data.put("lease", lease);
            return ResponseEntity.ok(ApiResponse.ok("续租成功", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            logger.error("工位续租异常: teamId={}", teamId, e);
            return ResponseEntity.status(500).body(ApiResponse.fail("续租失败，请稍后重试"));
        }
    }

    @PostMapping("/me/lease/release")
    public ResponseEntity<ApiResponse<Void>> release(HttpServletRequest request, HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamId == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        try {
            logger.info("工位释放请求: teamId={}", teamId);
            workstationService.releaseMyLeaseIfExpired(teamId);
            return ResponseEntity.ok(ApiResponse.ok("释放成功", null));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            logger.error("工位释放异常: teamId={}", teamId, e);
            return ResponseEntity.status(500).body(ApiResponse.fail("释放失败，请稍后重试"));
        }
    }

    public static class LeaseRequest {
        private Integer months;

        public Integer getMonths() {
            return months;
        }

        public void setMonths(Integer months) {
            this.months = months;
        }
    }
}
