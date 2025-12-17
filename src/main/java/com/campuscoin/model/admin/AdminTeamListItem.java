package com.campuscoin.model.admin;

import java.sql.Timestamp;

/**
 * 团队列表项视图对象
 * 用于后台管理系统的团队列表展示
 */
public class AdminTeamListItem {
    private Integer id;
    private String teamName;
    private String contactName;
    private String contactPhone;
    private Integer balance; // 当前虚拟币余额
    private Integer weeklyCheckins; // 本周活跃度（打卡次数）
    private Integer totalAchievements; // 成果数量（累计）
    private Integer weeklyResourceUsage; // 资源使用次数（本周）
    private Timestamp createdAt; // 注册时间

    public AdminTeamListItem() {
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

    public Integer getBalance() {
        return balance;
    }

    public void setBalance(Integer balance) {
        this.balance = balance;
    }

    public Integer getWeeklyCheckins() {
        return weeklyCheckins;
    }

    public void setWeeklyCheckins(Integer weeklyCheckins) {
        this.weeklyCheckins = weeklyCheckins;
    }

    public Integer getTotalAchievements() {
        return totalAchievements;
    }

    public void setTotalAchievements(Integer totalAchievements) {
        this.totalAchievements = totalAchievements;
    }

    public Integer getWeeklyResourceUsage() {
        return weeklyResourceUsage;
    }

    public void setWeeklyResourceUsage(Integer weeklyResourceUsage) {
        this.weeklyResourceUsage = weeklyResourceUsage;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
