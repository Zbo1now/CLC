package com.campuscoin.controller.admin;

import com.campuscoin.payload.ApiResponse;
import com.campuscoin.service.admin.AdminDashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private static final Logger logger = LoggerFactory.getLogger(AdminDashboardController.class);

    private final AdminDashboardService dashboardService;

    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> overview(HttpServletRequest request) {
        Object admin = request.getAttribute("adminUsername");
        String adminUsername = admin == null ? "unknown" : String.valueOf(admin);
        logger.info("管理员请求仪表盘数据: admin={}", adminUsername);

        Map<String, Object> data = dashboardService.buildOverview();
        logger.info("仪表盘数据生成完成: admin={}", adminUsername);
        return ResponseEntity.ok(ApiResponse.ok("获取成功", data));
    }
}
