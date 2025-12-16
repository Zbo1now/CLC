package com.campuscoin.model;

import java.sql.Timestamp;

public class DutyTaskSignup {
    private Integer id;
    private Integer taskId;
    private Integer teamId;
    private String signupStatus;
    private Timestamp signedAt;
    private Timestamp confirmedAt;
    private Integer rewardAmount;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public Integer getTeamId() {
        return teamId;
    }

    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }

    public String getSignupStatus() {
        return signupStatus;
    }

    public void setSignupStatus(String signupStatus) {
        this.signupStatus = signupStatus;
    }

    public Timestamp getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(Timestamp signedAt) {
        this.signedAt = signedAt;
    }

    public Timestamp getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Timestamp confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Integer getRewardAmount() {
        return rewardAmount;
    }

    public void setRewardAmount(Integer rewardAmount) {
        this.rewardAmount = rewardAmount;
    }
}
