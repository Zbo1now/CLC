package com.campuscoin.model;

import java.sql.Timestamp;

public class TrainingEvent {
    private Integer id;
    private String eventName;
    private String eventType;
    private Timestamp startTime;
    private Timestamp endTime;
    private String locationMode;
    private String locationDetail;
    private Integer rewardCoins;
    private Boolean requireCheckin;
    private String description;
    private String publishStatus;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // participation limits
    private Integer maxParticipants;
    private Integer currentParticipants;

    // computed fields for UI
    private String eventStatus; // NOT_STARTED / IN_PROGRESS / ENDED
    private String myParticipationStatus; // null / PENDING / APPROVED / REJECTED
    private Integer myParticipationId;
    private Boolean myProofSubmitted; // 是否已提交过证明材料

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }

    public String getLocationMode() {
        return locationMode;
    }

    public void setLocationMode(String locationMode) {
        this.locationMode = locationMode;
    }

    public String getLocationDetail() {
        return locationDetail;
    }

    public void setLocationDetail(String locationDetail) {
        this.locationDetail = locationDetail;
    }

    public Integer getRewardCoins() {
        return rewardCoins;
    }

    public void setRewardCoins(Integer rewardCoins) {
        this.rewardCoins = rewardCoins;
    }

    public Boolean getRequireCheckin() {
        return requireCheckin;
    }

    public void setRequireCheckin(Boolean requireCheckin) {
        this.requireCheckin = requireCheckin;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPublishStatus() {
        return publishStatus;
    }

    public void setPublishStatus(String publishStatus) {
        this.publishStatus = publishStatus;
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

    public Integer getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public Integer getCurrentParticipants() {
        return currentParticipants;
    }

    public void setCurrentParticipants(Integer currentParticipants) {
        this.currentParticipants = currentParticipants;
    }

    public String getEventStatus() {
        return eventStatus;
    }

    public void setEventStatus(String eventStatus) {
        this.eventStatus = eventStatus;
    }

    public String getMyParticipationStatus() {
        return myParticipationStatus;
    }

    public void setMyParticipationStatus(String myParticipationStatus) {
        this.myParticipationStatus = myParticipationStatus;
    }

    public Integer getMyParticipationId() {
        return myParticipationId;
    }

    public void setMyParticipationId(Integer myParticipationId) {
        this.myParticipationId = myParticipationId;
    }

    public Boolean getMyProofSubmitted() {
        return myProofSubmitted;
    }

    public void setMyProofSubmitted(Boolean myProofSubmitted) {
        this.myProofSubmitted = myProofSubmitted;
    }
}
