package com.campuscoin.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public final class LogUtil {
    private static volatile boolean configured = false;

    private LogUtil() {
    }

    public static Logger getLogger(Class<?> clazz) {
        configureOnce();
        return Logger.getLogger(clazz.getName());
    }

    private static void configureOnce() {
        if (configured) {
            return;
        }
        synchronized (LogUtil.class) {
            if (configured) {
                return;
            }
            try (InputStream in = LogUtil.class.getClassLoader().getResourceAsStream("logging.properties")) {
                if (in != null) {
                    LogManager.getLogManager().readConfiguration(in);
                }
            } catch (IOException e) {
                // fallback to defaults if logging config cannot be read
            }
            configured = true;
        }
    }
}
