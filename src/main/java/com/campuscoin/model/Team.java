package com.campuscoin.model;

import java.sql.Timestamp;

public class Team {
    private int id;
    private String teamName;
    private String passwordHash;
    private String contactName;
    private String contactPhone;
    private int balance;
    private Timestamp createdAt;

    public Team() {
    }

    public Team(int id, String teamName, String passwordHash, String contactName, String contactPhone, int balance, Timestamp createdAt) {
        this.id = id;
        this.teamName = teamName;
        this.passwordHash = passwordHash;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.balance = balance;
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

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
