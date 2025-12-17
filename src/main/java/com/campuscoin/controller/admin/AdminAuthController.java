package com.campuscoin.controller.admin;

import com.campuscoin.model.admin.AdminUser;
import com.campuscoin.payload.ApiResponse;
import com.campuscoin.payload.admin.AdminLoginRequest;
import com.campuscoin.service.admin.AdminAuthService;
import com.campuscoin.util.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
@Validated
public class AdminAuthController {

    private static final Logger logger = LoggerFactory.getLogger(AdminAuthController.class);

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody AdminLoginRequest req,
                                                                  HttpServletRequest request,
                                                                  HttpSession session) {
        String ip = request.getRemoteAddr();
        logger.info("管理员尝试登录: username={}, ip={}", req.getUsername(), ip);

        try {
            AdminUser user = adminAuthService.login(req.getUsername(), req.getPassword(), ip);

            session.setAttribute("adminId", user.getId());
            session.setAttribute("adminUsername", user.getUsername());
            session.setAttribute("adminDisplayName", user.getDisplayName());

            SessionManager.createSession(session.getId());
            SessionManager.setAttribute(session.getId(), "adminId", user.getId());
            SessionManager.setAttribute(session.getId(), "adminUsername", user.getUsername());
            SessionManager.setAttribute(session.getId(), "adminDisplayName", user.getDisplayName());

            Map<String, Object> data = new HashMap<>();
            data.put("username", user.getUsername());
            data.put("displayName", user.getDisplayName());
            data.put("sessionId", session.getId());

            logger.info("管理员登录成功: username={}, ip={}", user.getUsername(), ip);
            return ResponseEntity.ok(ApiResponse.ok("登录成功", data));
        } catch (IllegalArgumentException ex) {
            logger.warn("管理员登录失败: username={}, ip={}, reason={}", req.getUsername(), ip, ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail(ex.getMessage()));
        } catch (IllegalStateException ex) {
            logger.warn("管理员登录失败(禁用): username={}, ip={}, reason={}", req.getUsername(), ip, ex.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail(ex.getMessage()));
        } catch (Exception ex) {
            logger.error("管理员登录异常: username={}, ip={}", req.getUsername(), ip, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("登录失败，请稍后重试"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> logout(HttpServletRequest request) {
        String sessionId = SessionManager.getSessionId(request);
        if (sessionId != null) {
            SessionManager.invalidate(sessionId);
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(ApiResponse.ok("已退出", new HashMap<>()));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> me(HttpServletRequest request) {
        Map<String, Object> data = new HashMap<>();
        data.put("adminId", request.getAttribute("adminId"));
        data.put("username", request.getAttribute("adminUsername"));
        data.put("displayName", request.getAttribute("adminDisplayName"));
        return ResponseEntity.ok(ApiResponse.ok("获取成功", data));
    }
}
