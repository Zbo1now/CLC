package com.campuscoin.controller.admin;

import com.campuscoin.payload.ApiResponse;
import com.campuscoin.service.admin.AdminTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 后台管理系统 - 虚拟币流水管理 Controller
 * RESTful API 设计：GET /api/admin/transactions
 */
@RestController
@RequestMapping("/api/admin/transactions")
public class AdminTransactionController {

    private static final Logger logger = LoggerFactory.getLogger(AdminTransactionController.class);

    private final AdminTransactionService service;

    public AdminTransactionController(AdminTransactionService service) {
        this.service = service;
    }

    /**
     * 分页查询虚拟币流水（支持多条件筛选）
     *
     * @param teamName  团队名称（模糊搜索，可选）
     * @param txnTypes  变动类型（多选，逗号分隔，可选）
     * @param startDate 开始日期（YYYY-MM-DD，可选）
     * @param endDate   结束日期（YYYY-MM-DD，可选）
     * @param direction 金额方向（inflow/outflow/all，默认 all）
     * @param page      页码（从1开始，默认1）
     * @param pageSize  每页条数（默认20）
     * @return 分页结果
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> list(
            @RequestParam(required = false) String teamName,
            @RequestParam(required = false) List<String> txnTypes,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "all") String direction,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        logger.info("[Admin] 查询虚拟币流水：teamName={}, txnTypes={}, startDate={}, endDate={}, direction={}, page={}, pageSize={}",
                teamName, txnTypes, startDate, endDate, direction, page, pageSize);

        Timestamp startTime = null;
        Timestamp endTime = null;

        if (startDate != null) {
            startTime = Timestamp.valueOf(LocalDateTime.of(startDate, LocalTime.MIN));
        }
        if (endDate != null) {
            // 结束日期取次日零点，确保包含当天全天
            endTime = Timestamp.valueOf(LocalDateTime.of(endDate.plusDays(1), LocalTime.MIN));
        }

        Map<String, Object> result = service.listPaged(teamName, txnTypes, startTime, endTime, direction, page, pageSize);

        return ResponseEntity.ok(ApiResponse.ok("查询成功", result));
    }
}
