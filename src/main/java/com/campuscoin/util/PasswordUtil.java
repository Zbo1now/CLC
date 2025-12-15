package com.campuscoin.util;

import org.springframework.security.crypto.bcrypt.BCrypt;

public final class PasswordUtil {
    private PasswordUtil() {
    }

    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public static boolean verifyPassword(String password, String expectedHash) {
        return BCrypt.checkpw(password, expectedHash);
    }
}
