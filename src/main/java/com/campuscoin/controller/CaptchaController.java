package com.campuscoin.controller;

import com.campuscoin.payload.ApiResponse;
import com.campuscoin.service.CaptchaService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户端登录/注册验证码
 */
@RestController
@RequestMapping("/api/auth")
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @GetMapping("/captcha")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCaptcha() {
        CaptchaService.CaptchaResult r = captchaService.generate();

        Map<String, Object> data = new HashMap<>();
        data.put("captchaId", r.getCaptchaId());
        data.put("imageBase64", r.getImageBase64());
        data.put("expiresInSeconds", r.getExpiresInSeconds());

        // 防止缓存验证码
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok("获取成功", data));
    }
}


