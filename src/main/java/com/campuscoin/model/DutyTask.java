package com.campuscoin.model;

import java.sql.Timestamp;

public class DutyTask {
    private Integer id;
    private String taskName;
    private Timestamp startTime;
    private Timestamp endTime;
    private Integer requiredPeople;
    private Integer rewardCoins;
    private String taskDesc;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // computed fields for UI
    private String taskStatus; // NOT_STARTED / IN_PROGRESS / ENDED
    private Integer signupCount;
    private Integer remaining;
    private String mySignupStatus; // null / SIGNED / PENDING_CONFIRM / COMPLETED

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
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

    public Integer getRequiredPeople() {
        return requiredPeople;
    }

    public void setRequiredPeople(Integer requiredPeople) {
        this.requiredPeople = requiredPeople;
    }

    public Integer getRewardCoins() {
        return rewardCoins;
    }

    public void setRewardCoins(Integer rewardCoins) {
        this.rewardCoins = rewardCoins;
    }

    public String getTaskDesc() {
        return taskDesc;
    }

    public void setTaskDesc(String taskDesc) {
        this.taskDesc = taskDesc;
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

    public String getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(String taskStatus) {
        this.taskStatus = taskStatus;
    }

    public Integer getSignupCount() {
        return signupCount;
    }

    public void setSignupCount(Integer signupCount) {
        this.signupCount = signupCount;
    }

    public Integer getRemaining() {
        return remaining;
    }

    public void setRemaining(Integer remaining) {
        this.remaining = remaining;
    }

    public String getMySignupStatus() {
        return mySignupStatus;
    }

    public void setMySignupStatus(String mySignupStatus) {
        this.mySignupStatus = mySignupStatus;
    }
}
