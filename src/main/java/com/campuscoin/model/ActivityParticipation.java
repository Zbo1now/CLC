package com.campuscoin.model;

import java.sql.Timestamp;

/**
 * 活动参与记录实体类
 */
public class ActivityParticipation {
    private Integer id;
    private Integer activityId;
    private Integer teamId;
    private String teamName;
    private Timestamp applyTime;
    private String status; // PENDING, APPROVED, REJECTED, COMPLETED
    private String reviewStatus; // PENDING, APPROVED, REJECTED
    private String reviewedBy;
    private Timestamp reviewedAt;
    private String rejectReason;
    private Integer coinsRewarded;
    private Timestamp coinsRewardedAt;
    private String completionNotes;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public ActivityParticipation() {
    }

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

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
