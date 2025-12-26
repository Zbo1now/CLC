package com.campuscoin.model.admin;

import java.sql.Timestamp;

/**
 * 管理员端参与记录列表项（包含团队和活动信息）
 */
public class AdminParticipationListItem {
        // 标准化后的材料url（BOS公有读域名型）
        private String proofUrl;
    private Integer id;
    private Integer activityId;
    private String activityName;
    private Integer teamId;
    private String teamName;
    private Timestamp applyTime;
    private String status;
    private String reviewStatus;
    private String reviewedBy;
    private Timestamp reviewedAt;
    private String rejectReason;
    private Integer coinsRewarded;
    private Timestamp coinsRewardedAt;
    private String completionNotes;

    /**
     * 自动从completionNotes中提取材料链接（proofUrl），并清洗前缀，兼容历史数据。
     * 只提取“证明材料：”后第一个非空字符串，去除多余空格和换行。
     */
    public String getProofUrl() {
        // 优先返回标准化后的proofUrl
        if (proofUrl != null) return proofUrl;
        if (completionNotes == null) return null;
        String notes = completionNotes.trim();
        int idx = notes.indexOf("证明材料：");
        if (idx == -1) return null;
        String rest = notes.substring(idx + 5);
        int endIdx = rest.indexOf("\n");
        String url = (endIdx == -1 ? rest : rest.substring(0, endIdx)).trim();
        if (url.isEmpty()) return null;
        return url;
    }

    public void setProofUrl(String proofUrl) {
        this.proofUrl = proofUrl;
    }

    public AdminParticipationListItem() {
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getActivityId() {
        return activityId;
    }

    public void setActivityId(Integer activityId) {
        this.activityId = activityId;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public Integer getTeamId() {
        return teamId;
    }

    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public Timestamp getApplyTime() {
        return applyTime;
    }

    public void setApplyTime(Timestamp applyTime) {
        this.applyTime = applyTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public Timestamp getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Timestamp reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public Integer getCoinsRewarded() {
        return coinsRewarded;
    }

    public void setCoinsRewarded(Integer coinsRewarded) {
        this.coinsRewarded = coinsRewarded;
    }

    public Timestamp getCoinsRewardedAt() {
        return coinsRewardedAt;
    }

    public void setCoinsRewardedAt(Timestamp coinsRewardedAt) {
        this.coinsRewardedAt = coinsRewardedAt;
    }

    public String getCompletionNotes() {
        return completionNotes;
    }

    public void setCompletionNotes(String completionNotes) {
        this.completionNotes = completionNotes;
    }
}
