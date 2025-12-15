package com.campuscoin.controller;

import com.campuscoin.model.Team;
import com.campuscoin.payload.ApiResponse;
import com.campuscoin.service.AuthService;
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

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@RequestBody RegisterRequest req) {
        try {
            Team team = authService.register(req.getTeamName(), req.getPassword(), req.getContactName(), req.getContactPhone());
            Map<String, Object> data = new HashMap<>();
            data.put("teamName", team.getTeamName());
            data.put("balance", team.getBalance());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("注册成功", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (IllegalStateException e) {
            HttpStatus status = "团队名称已存在".equals(e.getMessage()) ? HttpStatus.CONFLICT : HttpStatus.INTERNAL_SERVER_ERROR;
            return ResponseEntity.status(status).body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody LoginRequest req, HttpSession session) {
        try {
            Team team = authService.login(req.getTeamName(), req.getPassword());
            session.setAttribute("teamId", team.getId());
            session.setAttribute("teamName", team.getTeamName());
            Map<String, Object> data = new HashMap<>();
            data.put("teamName", team.getTeamName());
            data.put("balance", team.getBalance());
            return ResponseEntity.ok(ApiResponse.ok("登录成功", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail(e.getMessage()));
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
    }

    public static class LoginRequest {
        @NotBlank
        private String teamName;
        @NotBlank
        private String password;

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
    }
}
