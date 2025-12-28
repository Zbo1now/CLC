package com.campuscoin.controller;

import com.campuscoin.model.Team;
import com.campuscoin.payload.ApiResponse;
import com.campuscoin.service.AuthService;
import com.campuscoin.service.CaptchaService;
import com.campuscoin.util.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    private final CaptchaService captchaService;

    public AuthController(AuthService authService, CaptchaService captchaService) {
        this.authService = authService;
        this.captchaService = captchaService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@RequestBody RegisterRequest req) {
        logger.info("尝试注册团队: {}", req.getTeamName());
        try {
            boolean captchaOk = captchaService.verify(req.getCaptchaId(), req.getCaptchaCode());
            if (!captchaOk) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("验证码错误或已过期"));
            }

            Team team = authService.register(req.getTeamName(), req.getPassword(), req.getContactName(), req.getContactPhone());
            Map<String, Object> data = new HashMap<>();
            data.put("teamName", team.getTeamName());
            data.put("balance", team.getBalance());
            logger.info("团队注册成功: {}", team.getTeamName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("注册成功", data));
        } catch (IllegalArgumentException e) {
            logger.error("注册失败 (参数错误): {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (IllegalStateException e) {
            logger.error("注册失败 (状态错误): {}", e.getMessage());
            HttpStatus status = "团队名称已存在".equals(e.getMessage()) ? HttpStatus.CONFLICT : HttpStatus.INTERNAL_SERVER_ERROR;
            return ResponseEntity.status(status).body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody LoginRequest req, HttpSession session) {
        logger.info("尝试登录团队: {}", req.getTeamName());
        try {
            boolean captchaOk = captchaService.verify(req.getCaptchaId(), req.getCaptchaCode());
            if (!captchaOk) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("验证码错误或已过期"));
            }

            Team team = authService.login(req.getTeamName(), req.getPassword());
            
            // 1. 标准 Session 存储
            session.setAttribute("teamId", team.getId());
            session.setAttribute("teamName", team.getTeamName());
            
            // 2. 自定义 SessionManager 存储 (用于 Header 传递)
            SessionManager.createSession(session.getId());
            SessionManager.setAttribute(session.getId(), "teamId", team.getId());
            SessionManager.setAttribute(session.getId(), "teamName", team.getTeamName());

            Map<String, Object> data = new HashMap<>();
            data.put("teamName", team.getTeamName());
            data.put("balance", team.getBalance());
            // 返回 Session ID 供前端手动维护
            data.put("sessionId", session.getId());
            
            logger.info("团队登录成功: {}", team.getTeamName());
            return ResponseEntity.ok(ApiResponse.ok("登录成功", data));
        } catch (IllegalArgumentException e) {
            logger.warn("团队登录失败: {}. 原因: {}", req.getTeamName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail(e.getMessage()));
        } catch (IllegalStateException e) {
            logger.warn("团队登录失败(禁用): {}. 原因: {}", req.getTeamName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail(e.getMessage()));
        }
    }

    public static class RegisterRequest {
        @NotBlank
        private String teamName;
        @NotBlank
        @Size(min = 6)
        private String password;
        @NotBlank
        private String contactName;
        @NotBlank
        private String contactPhone;
        @NotBlank
        private String captchaId;
        @NotBlank
        private String captchaCode;

        public String getTeamName() {
            return teamName;
        }

        public void setTeamName(String teamName) {
            this.teamName = teamName;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getContactName() {
            return contactName;
        }

        public void setContactName(String contactName) {
            this.contactName = contactName;
        }

        public String getContactPhone() {
            return contactPhone;
        }

        public void setContactPhone(String contactPhone) {
            this.contactPhone = contactPhone;
        }

        public String getCaptchaId() {
            return captchaId;
        }

        public void setCaptchaId(String captchaId) {
            this.captchaId = captchaId;
        }

        public String getCaptchaCode() {
            return captchaCode;
        }

        public void setCaptchaCode(String captchaCode) {
            this.captchaCode = captchaCode;
        }
    }

    public static class LoginRequest {
        @NotBlank
        private String teamName;
        @NotBlank
        private String password;
        @NotBlank
        private String captchaId;
        @NotBlank
        private String captchaCode;

        public String getTeamName() {
            return teamName;
        }

        public void setTeamName(String teamName) {
            this.teamName = teamName;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getCaptchaId() {
            return captchaId;
        }

        public void setCaptchaId(String captchaId) {
            this.captchaId = captchaId;
        }

        public String getCaptchaCode() {
            return captchaCode;
        }

        public void setCaptchaCode(String captchaCode) {
            this.captchaCode = captchaCode;
        }
    }
}
