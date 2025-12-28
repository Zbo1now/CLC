package com.campuscoin.service;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简易图形验证码服务（内存存储，带过期时间）
 */
@Service
public class CaptchaService {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LEN = 4;
    private static final long EXPIRE_MS = 5 * 60 * 1000L;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, CaptchaEntry> store = new ConcurrentHashMap<>();

    public CaptchaResult generate() {
        cleanupExpired();

        String code = randomDigits(CODE_LEN);
        String captchaId = UUID.randomUUID().toString().replace("-", "");

        String base64 = renderBase64Png(code);
        store.put(captchaId, new CaptchaEntry(code, System.currentTimeMillis() + EXPIRE_MS));
        return new CaptchaResult(captchaId, base64, (int) (EXPIRE_MS / 1000));
    }

    public boolean verify(String captchaId, String captchaCode) {
        if (captchaId == null || captchaId.trim().isEmpty()) {
            return false;
        }
        if (captchaCode == null || captchaCode.trim().isEmpty()) {
            return false;
        }

        CaptchaEntry entry = store.get(captchaId.trim());
        if (entry == null) {
            return false;
        }
        if (System.currentTimeMillis() > entry.expiresAtMs) {
            store.remove(captchaId.trim());
            return false;
        }

        boolean ok = entry.code.equalsIgnoreCase(captchaCode.trim());
        // 一次性使用，避免重放
        store.remove(captchaId.trim());
        return ok;
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> now > e.getValue().expiresAtMs);
    }

    private String randomDigits(int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private String renderBase64Png(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 背景
            g.setColor(new Color(245, 247, 251));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            // 干扰线
            for (int i = 0; i < 6; i++) {
                g.setColor(new Color(180 + random.nextInt(60), 180 + random.nextInt(60), 180 + random.nextInt(60)));
                int x1 = random.nextInt(WIDTH);
                int y1 = random.nextInt(HEIGHT);
                int x2 = random.nextInt(WIDTH);
                int y2 = random.nextInt(HEIGHT);
                g.drawLine(x1, y1, x2, y2);
            }

            // 文本
            g.setFont(new Font("Arial", Font.BOLD, 26));
            for (int i = 0; i < code.length(); i++) {
                g.setColor(new Color(50 + random.nextInt(120), 50 + random.nextInt(120), 50 + random.nextInt(120)));
                int x = 18 + i * 22 + random.nextInt(4);
                int y = 28 + random.nextInt(6);
                g.drawString(String.valueOf(code.charAt(i)), x, y);
            }

            // 噪点
            for (int i = 0; i < 120; i++) {
                g.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
                int x = random.nextInt(WIDTH);
                int y = random.nextInt(HEIGHT);
                g.fillRect(x, y, 1, 1);
            }
        } finally {
            g.dispose();
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception ex) {
            throw new RuntimeException("生成验证码失败", ex);
        }
    }

    private static class CaptchaEntry {
        private final String code;
        private final long expiresAtMs;

        private CaptchaEntry(String code, long expiresAtMs) {
            this.code = code;
            this.expiresAtMs = expiresAtMs;
        }
    }

    public static class CaptchaResult {
        private final String captchaId;
        private final String imageBase64;
        private final int expiresInSeconds;

        public CaptchaResult(String captchaId, String imageBase64, int expiresInSeconds) {
            this.captchaId = captchaId;
            this.imageBase64 = imageBase64;
            this.expiresInSeconds = expiresInSeconds;
        }

        public String getCaptchaId() {
            return captchaId;
        }

        public String getImageBase64() {
            return imageBase64;
        }

        public int getExpiresInSeconds() {
            return expiresInSeconds;
        }
    }
}


