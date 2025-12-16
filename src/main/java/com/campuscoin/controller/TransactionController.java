package com.campuscoin.controller;

import com.campuscoin.payload.ApiResponse;
import com.campuscoin.service.TransactionService;
import com.campuscoin.util.SessionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // 明细页：查询当前团队所有流水（分页）
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> myTransactions(
            HttpServletRequest request,
            HttpSession session,
            @RequestParam(value = "offset", required = false, defaultValue = "0") int offset,
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit
    ) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamId == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        Map<String, Object> data = transactionService.listMyTransactions(teamId, offset, limit);
        logger.info("查询我的流水: teamId={}, offset={}, limit={}", teamId, offset, limit);
        return ResponseEntity.ok(ApiResponse.ok("获取成功", data));
    }
}
