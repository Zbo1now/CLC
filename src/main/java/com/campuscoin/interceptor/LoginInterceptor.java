package com.campuscoin.interceptor;

import com.campuscoin.payload.ApiResponse;
import com.campuscoin.util.SessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.PrintWriter;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LoginInterceptor.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        
        // 如果是 OPTIONS 请求，直接放行（CORS 预检）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 1. 尝试获取 SessionID (Header 优先)
        String sessionId = SessionManager.getSessionId(request);
        
        // 2. 检查登录状态 (先查自定义 SessionManager，再查标准 HttpSession)
        boolean isLoggedIn = false;
        
        if (sessionId != null) {
            // 检查自定义存储
            Object teamName = SessionManager.getAttribute(sessionId, "teamName");
            if (teamName != null) {
                isLoggedIn = true;
                // 将自定义 Session 数据同步到 request attribute，方便 Controller 使用
                request.setAttribute("teamName", teamName);
                request.setAttribute("teamId", SessionManager.getAttribute(sessionId, "teamId"));
            } else {
                // 检查标准 HttpSession (兼容 Cookie 方式)
                HttpSession session = request.getSession(false);
                if (session != null && session.getAttribute("teamName") != null) {
                    isLoggedIn = true;
                }
            }
        }

        logger.info("拦截器检查: URI={}, Method={}, SessionID={}, 已登录={}", 
            uri, request.getMethod(), sessionId, isLoggedIn);

        if (isLoggedIn) {
            return true;
        }

        // 未登录，返回 401 JSON
        logger.warn("拦截器拒绝: 未登录访问 {}", uri);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        
        PrintWriter out = response.getWriter();
        String json = objectMapper.writeValueAsString(ApiResponse.fail("请先登录"));
        out.print(json);
        out.flush();
        
        return false;
    }
}
