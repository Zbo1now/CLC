package com.campuscoin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "baidu")
public class BaiduConfig {
    private FaceConfig face;
    private BosConfig bos;

    public FaceConfig getFace() {
        return face;
    }

    public void setFace(FaceConfig face) {
        this.face = face;
    }

    public BosConfig getBos() {
        return bos;
    }

    public void setBos(BosConfig bos) {
        this.bos = bos;
    }

    public static class FaceConfig {
        private String appId;
        private String apiKey;
        private String secretKey;
        private String groupId;

        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public String getGroupId() { return groupId; }
        public void setGroupId(String groupId) { this.groupId = groupId; }
    }

    public static class BosConfig {
        private String accessKeyId;
        private String secretAccessKey;
        private String endpoint;
        private String bucketName;

        public String getAccessKeyId() { return accessKeyId; }
        public void setAccessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; }
        public String getSecretAccessKey() { return secretAccessKey; }
        public void setSecretAccessKey(String secretAccessKey) { this.secretAccessKey = secretAccessKey; }
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getBucketName() { return bucketName; }
        public void setBucketName(String bucketName) { this.bucketName = bucketName; }
    }
}
