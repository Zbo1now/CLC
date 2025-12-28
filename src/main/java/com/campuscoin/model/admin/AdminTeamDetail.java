package com.campuscoin.model.admin;

import java.sql.Timestamp;
import java.util.List;

/**
 * 团队详情视图对象
 * 包含基础信息和统计数据，用于能力画像
 */
public class AdminTeamDetail {
    private Integer id;
    private String teamName;
    private String contactName;
    private String contactPhone;
    private String faceImage;
    private Boolean enabled; // 账号状态
    private Integer balance;
    private Integer currentStreak;
    private Timestamp createdAt;

    // 能力雷达图数据
    private Integer innovationScore; // 创新力（成果数量 × 奖励值）
    private Integer activityScore; // 活跃度（打卡频率）
    private Integer resourceScore; // 资源利用率（消费合理性）
    private Integer participationScore; // 参与度（值班+培训次数）

    // 统计数据
    private Integer totalAchievements; // 累计成果数
    private Integer totalCheckins; // 累计打卡次数
    private Integer totalDutyTasks; // 累计值班次数
    private Integer totalTrainings; // 累计培训次数
    private Integer weeklyResourceUsage; // 本周资源使用次数

    // 成员列表（可选，如果有成员表）
    private List<String> members;

    public AdminTeamDetail() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getFaceImage() {
        return faceImage;
    }

    public void setFaceImage(String faceImage) {
        this.faceImage = faceImage;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getBalance() {
        return balance;
    }

    public void setBalance(Integer balance) {
        this.balance = balance;
    }

    public Integer getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(Integer currentStreak) {
        this.currentStreak = currentStreak;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getInnovationScore() {
        return innovationScore;
    }

    public void setInnovationScore(Integer innovationScore) {
        this.innovationScore = innovationScore;
    }

    public Integer getActivityScore() {
        return activityScore;
    }

    public void setActivityScore(Integer activityScore) {
        this.activityScore = activityScore;
    }

    public Integer getResourceScore() {
        return resourceScore;
    }

    public void setResourceScore(Integer resourceScore) {
        this.resourceScore = resourceScore;
    }

    public Integer getParticipationScore() {
        return participationScore;
    }

    public void setParticipationScore(Integer participationScore) {
        this.participationScore = participationScore;
    }

    public Integer getTotalAchievements() {
        return totalAchievements;
    }

    public void setTotalAchievements(Integer totalAchievements) {
        this.totalAchievements = totalAchievements;
    }

    public Integer getTotalCheckins() {
        return totalCheckins;
    }

    public void setTotalCheckins(Integer totalCheckins) {
        this.totalCheckins = totalCheckins;
    }

    public Integer getTotalDutyTasks() {
        return totalDutyTasks;
    }

    public void setTotalDutyTasks(Integer totalDutyTasks) {
        this.totalDutyTasks = totalDutyTasks;
    }

    public Integer getTotalTrainings() {
        return totalTrainings;
    }

    public void setTotalTrainings(Integer totalTrainings) {
        this.totalTrainings = totalTrainings;
    }

    public Integer getWeeklyResourceUsage() {
        return weeklyResourceUsage;
    }

    public void setWeeklyResourceUsage(Integer weeklyResourceUsage) {
        this.weeklyResourceUsage = weeklyResourceUsage;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }
}
