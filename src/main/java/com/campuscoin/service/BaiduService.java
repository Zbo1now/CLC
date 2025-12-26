
    package com.campuscoin.service;

import com.baidu.aip.face.AipFace;
import com.baidu.aip.face.MatchRequest;
import com.baidubce.auth.DefaultBceCredentials;
import com.baidubce.services.bos.BosClient;
import com.baidubce.services.bos.BosClientConfiguration;
import com.campuscoin.config.BaiduConfig;
import com.campuscoin.util.LogUtil;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.baidubce.services.bos.model.BosObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.logging.Logger;

@Service
public class BaiduService {

    /**
     * 将路径型BOS地址（如https://bj.bcebos.com/bucket/key）转为域名型（如https://bucket.bj.bcebos.com/key）
     * 兼容key、路径型、域名型、全路径等多种输入
     */
    public String toBosPublicUrl(String urlOrKey) {
        if (urlOrKey == null || urlOrKey.trim().isEmpty()) return null;
        String key = urlOrKey.trim();
        String bucket = config.getBos().getBucketName();
        String endpoint = config.getBos().getEndpoint();
        if (endpoint.endsWith("/")) endpoint = endpoint.substring(0, endpoint.length() - 1);
        String domain = "https://" + bucket + "." + endpoint.replace("http://", "").replace("https://", "");
        // 已是域名型
        if (key.startsWith("http") && key.contains(bucket + ".")) return key;
        // 路径型 http(s)://bj.bcebos.com/bucket/key
        String regex = "https?://bj\\.bcebos\\.com/" + bucket + "/(.+)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher m = p.matcher(key);
        if (m.matches()) {
            String realKey = m.group(1);
            return domain + "/" + realKey;
        }
        // 只给了key
        if (!key.startsWith("http")) {
            return domain + "/" + key.replaceAll("^/+", "");
        }
        // 兜底：原样返回
        return key;
    }

        /**
         * 生成BOS对象的临时签名下载链接（默认10分钟有效）
         */
        public String generateBosSignedUrl(String fileName, int expireSeconds) {
            try {
                com.baidubce.services.bos.model.GeneratePresignedUrlRequest req =
                    new com.baidubce.services.bos.model.GeneratePresignedUrlRequest(
                        config.getBos().getBucketName(), fileName);
                req.setMethod(com.baidubce.http.HttpMethodName.GET);
                req.setExpiration(expireSeconds); // 直接传int秒数
                java.net.URL url = bosClient.generatePresignedUrl(req);
                return url.toString();
            } catch (Exception e) {
                logger.severe("Generate BOS SignedUrl Failed: " + e.getMessage());
                return null;
            }
        }
    
        /**
         * 根据完整的 proofUrl 提取 key，生成签名链接，兼容有路径前缀的文件名
         */
        public String generateBosSignedUrlFromUrl(String proofUrl, int expireSeconds) {
            String key = extractKeyFromUrl(proofUrl);
            if (key == null) return null;
            try {
                return generateBosSignedUrl(key, expireSeconds);
            } catch (Exception e) {
                logger.severe("Generate BOS SignedUrl FromUrl Failed: " + e.getMessage());
                return null;
            }
        }

        /**
         * 解析 proofUrl 得到对象 key（不含 bucket）。兼容 http/https 和相对路径。
         */
        public String extractKeyFromUrl(String proofUrl) {
            if (proofUrl == null || proofUrl.trim().isEmpty()) return null;
            String key = proofUrl.trim();
            boolean isHttp = key.startsWith("http://") || key.startsWith("https://");
            if (isHttp) {
                int idxBucket = key.indexOf(config.getBos().getBucketName() + "/");
                if (idxBucket != -1) {
                    key = key.substring(idxBucket + config.getBos().getBucketName().length() + 1);
                } else {
                    int idx = key.indexOf("//");
                    String path = idx != -1 ? key.substring(idx + 2) : key;
                    int firstSlash = path.indexOf('/');
                    if (firstSlash != -1) {
                        key = path.substring(firstSlash + 1);
                    }
                }
            } else {
                if (key.startsWith("/")) {
                    key = key.substring(1);
                }
            }
            if (key.isEmpty()) return null;
            return key;
        }

        /**
         * 直接获取 BOS 对象（流），用于后端代理下载
         */
        public BosObject getObject(String key) {
            return bosClient.getObject(config.getBos().getBucketName(), key);
        }
    private static final Logger logger = LogUtil.getLogger(BaiduService.class);
    
    private final AipFace aipFace;
    private final BosClient bosClient;
    private final BaiduConfig config;

    public BaiduService(BaiduConfig config) {
        this.config = config;
        
        // 初始化人脸识别客户端
        this.aipFace = new AipFace(
            config.getFace().getAppId(),
            config.getFace().getApiKey(),
            config.getFace().getSecretKey()
        );
        
        // 初始化 BOS 客户端
        BosClientConfiguration bosConfig = new BosClientConfiguration();
        bosConfig.setCredentials(new DefaultBceCredentials(
            config.getBos().getAccessKeyId(),
            config.getBos().getSecretAccessKey()
        ));
        bosConfig.setEndpoint(config.getBos().getEndpoint());
        this.bosClient = new BosClient(bosConfig);
    }

    /**
     * 注册人脸到百度人脸库
     */
    public boolean registerFace(String userId, String imageBase64) {
        // 如果存在前缀则去除
        if (imageBase64.contains(",")) {
            imageBase64 = imageBase64.split(",")[1];
        }

        HashMap<String, String> options = new HashMap<>();
        options.put("user_info", "registered_user");
        options.put("quality_control", "NORMAL");
        options.put("liveness_control", "LOW"); // 根据需求调整

        JSONObject res = aipFace.addUser(
            imageBase64, 
            "BASE64", 
            config.getFace().getGroupId(), 
            userId, 
            options
        );

        logger.info("Baidu Face Register Response: " + res.toString());
        return res.has("error_code") && res.getInt("error_code") == 0;
    }

    /**
     * 在人脸库中搜索人脸
     * 如果找到高置信度的匹配，返回 userId，否则返回 null
     */
    public String searchFace(String imageBase64) {
        if (imageBase64.contains(",")) {
            imageBase64 = imageBase64.split(",")[1];
        }

        HashMap<String, Object> options = new HashMap<>();
        options.put("quality_control", "NORMAL");
        options.put("liveness_control", "LOW");
        options.put("max_user_num", "1");

        JSONObject res = aipFace.search(
            imageBase64, 
            "BASE64", 
            config.getFace().getGroupId(), 
            options
        );

        logger.info("Baidu Face Search Response: " + res.toString());

        if (res.has("error_code") && res.getInt("error_code") == 0) {
            JSONObject result = res.getJSONObject("result");
            if (result.has("user_list")) {
                JSONObject bestMatch = result.getJSONArray("user_list").getJSONObject(0);
                double score = bestMatch.getDouble("score");
                if (score > 80) { // 置信度阈值
                    return bestMatch.getString("user_id");
                }
            }
        }
        return null;
    }

    /**
     * 人脸比对 (1:1)
     * @param imageBase64 现场采集的人脸
     * @param remoteUrl 数据库中存储的基准人脸URL
     */
    public boolean matchFace(String imageBase64, String remoteUrl) {
        if (imageBase64.contains(",")) {
            imageBase64 = imageBase64.split(",")[1];
        }

        MatchRequest req1 = new MatchRequest(imageBase64, "BASE64");
        MatchRequest req2 = new MatchRequest(remoteUrl, "URL");
        
        ArrayList<MatchRequest> requests = new ArrayList<>();
        requests.add(req1);
        requests.add(req2);

        try {
            JSONObject res = aipFace.match(requests);
            logger.info("Baidu Face Match Response: " + res.toString());

            if (res.has("result") && !res.isNull("result")) {
                JSONObject result = res.getJSONObject("result");
                double score = result.getDouble("score");
                // 推荐阈值80，可根据实际情况调整
                return score > 80;
            }
        } catch (Exception e) {
            logger.severe("Face Match Failed: " + e.getMessage());
        }
        return false;
    }

    /**
     * 上传图片到 BOS
     */
    public String uploadToBos(String imageBase64, String fileName) {
        try {
            if (imageBase64.contains(",")) {
                imageBase64 = imageBase64.split(",")[1];
            }
            byte[] bytes = Base64.getDecoder().decode(imageBase64);
            
            bosClient.putObject(
                config.getBos().getBucketName(), 
                fileName, 
                new ByteArrayInputStream(bytes)
            );
            
            return config.getBos().getEndpoint() + "/" + config.getBos().getBucketName() + "/" + fileName;
        } catch (Exception e) {
            logger.severe("BOS Upload Failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * 上传文件到 BOS (支持 MultipartFile)
     */
    public String uploadToBos(MultipartFile file, String fileName) {
        try (InputStream in = file.getInputStream()) {
            bosClient.putObject(
                config.getBos().getBucketName(),
                fileName,
                in
            );
            return config.getBos().getEndpoint() + "/" + config.getBos().getBucketName() + "/" + fileName;
        } catch (Exception e) {
            logger.severe("BOS Upload Failed: " + e.getMessage());
            return null;
        }
    }
}
