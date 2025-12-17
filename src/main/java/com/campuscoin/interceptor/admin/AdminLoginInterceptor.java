package com.campuscoin.interceptor.admin;

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
public class AdminLoginInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AdminLoginInterceptor.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String sessionId = SessionManager.getSessionId(request);

        boolean isLoggedIn = false;
        if (sessionId != null) {
            Object adminUsername = SessionManager.getAttribute(sessionId, "adminUsername");
            if (adminUsername != null) {
                isLoggedIn = true;
                request.setAttribute("adminId", SessionManager.getAttribute(sessionId, "adminId"));
                request.setAttribute("adminUsername", adminUsername);
                request.setAttribute("adminDisplayName", SessionManager.getAttribute(sessionId, "adminDisplayName"));
            } else {
                HttpSession session = request.getSession(false);
                if (session != null && session.getAttribute("adminUsername") != null) {
                    isLoggedIn = true;
                    request.setAttribute("adminId", session.getAttribute("adminId"));
                    request.setAttribute("adminUsername", session.getAttribute("adminUsername"));
                    request.setAttribute("adminDisplayName", session.getAttribute("adminDisplayName"));
                }
            }
        }

        logger.info("Admin拦截器检查: URI={}, Method={}, SessionID={}, 已登录={}", uri, request.getMethod(), sessionId, isLoggedIn);

        if (isLoggedIn) {
            return true;
        }

        logger.warn("Admin拦截器拒绝: 未登录访问 {}", uri);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        PrintWriter out = response.getWriter();
        String json = objectMapper.writeValueAsString(ApiResponse.fail("请先登录"));
        out.print(json);
        out.flush();

        return false;
    }
}
