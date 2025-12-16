package com.campuscoin.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Utility helpers for resolving logged-in team attributes from session or request.
 */
public final class SessionHelper {

    private SessionHelper() {
        // utility class
    }

    public static String resolveTeamName(HttpServletRequest request, HttpSession session) {
        if (session != null) {
            Object attr = session.getAttribute("teamName");
            if (attr instanceof String) {
                return (String) attr;
            }
        }
        if (request != null) {
            Object attr = request.getAttribute("teamName");
            if (attr instanceof String) {
                return (String) attr;
            }
        }
        return null;
    }

    public static Integer resolveTeamId(HttpServletRequest request, HttpSession session) {
        if (session != null) {
            Object attr = session.getAttribute("teamId");
            if (attr instanceof Integer) {
                return (Integer) attr;
            }
        }
        if (request != null) {
            Object attr = request.getAttribute("teamId");
            if (attr instanceof Integer) {
                return (Integer) attr;
            }
        }
        return null;
    }
}