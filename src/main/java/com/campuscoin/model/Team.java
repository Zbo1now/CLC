package com.campuscoin.model;

import java.sql.Timestamp;

public class Team {
    private int id;
    private String teamName;
    private String passwordHash;
    private String contactName;
    private String contactPhone;
    private String faceImage;
    private int balance;
    private int currentStreak;
    private java.sql.Date lastCheckinDate;
    private Timestamp createdAt;

    public Team() {
    }

    public Team(int id, String teamName, String passwordHash, String contactName, String contactPhone, String faceImage, int balance, int currentStreak, java.sql.Date lastCheckinDate, Timestamp createdAt) {
        this.id = id;
        this.teamName = teamName;
        this.passwordHash = passwordHash;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.faceImage = faceImage;
        this.balance = balance;
        this.currentStreak = currentStreak;
        this.lastCheckinDate = lastCheckinDate;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }


    public void setId(int id) {
        this.id = id;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
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

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public java.sql.Date getLastCheckinDate() {
        return lastCheckinDate;
    }

    public void setLastCheckinDate(java.sql.Date lastCheckinDate) {
        this.lastCheckinDate = lastCheckinDate;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
