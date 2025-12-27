package com.campuscoin.config;

import com.campuscoin.interceptor.LoginInterceptor;
import com.campuscoin.interceptor.admin.AdminLoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Autowired
    private AdminLoginInterceptor adminLoginInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*") // 允许所有来源
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS") // 允许的HTTP方法
                .allowedHeaders("*") // 允许所有头信息
                .allowCredentials(true) // 允许携带凭证（如Cookie）
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**") // 拦截所有 API
                .excludePathPatterns(
                        "/api/auth/**",      // 排除登录注册
                "/api/face/login",   // 排除刷脸登录
                "/api/admin/**"      // 排除后台管理端 API（由 AdminLoginInterceptor 负责）
                );

        registry.addInterceptor(adminLoginInterceptor)
            .addPathPatterns("/api/admin/**")
            .excludePathPatterns(
                "/api/admin/auth/login",
                "/api/admin/auth/logout"
            );
    }
}
