package com.campuscoin.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义 Session 管理器
 * 用于解决跨域 Cookie 丢失问题，允许通过 Header 传递 SessionID
 */
public class SessionManager {
    
    // 内存中存储 Session 数据：Map<SessionId, Map<AttributeName, AttributeValue>>
    private static final Map<String, Map<String, Object>> SESSION_STORE = new ConcurrentHashMap<>();
    
    public static void createSession(String sessionId) {
        SESSION_STORE.putIfAbsent(sessionId, new ConcurrentHashMap<>());
    }
    
    public static void setAttribute(String sessionId, String key, Object value) {
        Map<String, Object> session = SESSION_STORE.get(sessionId);
        if (session == null) {
            session = new ConcurrentHashMap<>();
            SESSION_STORE.put(sessionId, session);
        }
        session.put(key, value);
    }
    
    public static Object getAttribute(String sessionId, String key) {
        Map<String, Object> session = SESSION_STORE.get(sessionId);
        return session != null ? session.get(key) : null;
    }
    
    public static void invalidate(String sessionId) {
        SESSION_STORE.remove(sessionId);
    }
    
    /**
     * 尝试从 Header 或 Cookie 获取 SessionID
     */
    public static String getSessionId(HttpServletRequest request) {
        // 1. 优先从 Header 获取
        String headerId = request.getHeader("X-Session-Id");
        if (headerId != null && !headerId.isEmpty()) {
            return headerId;
        }
        
        // 2. 其次尝试标准 Session (Cookie)
        HttpSession session = request.getSession(false);
        if (session != null) {
            return session.getId();
        }
        
        return null;
    }
}
