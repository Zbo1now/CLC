package com.campuscoin.model;

import java.sql.Date;
import java.sql.Timestamp;

public class WorkstationLease {
    private int id;
    private int workstationId;
    private int teamId;
    private Date startMonth;
    private Date endMonth;
    private int monthlyRent;
    private int totalCost;
    private String status;
    private Timestamp releasedAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getWorkstationId() {
        return workstationId;
    }

    public void setWorkstationId(int workstationId) {
        this.workstationId = workstationId;
    }

    public int getTeamId() {
        return teamId;
    }

    public void setTeamId(int teamId) {
        this.teamId = teamId;
    }

    public Date getStartMonth() {
        return startMonth;
    }

    public void setStartMonth(Date startMonth) {
        this.startMonth = startMonth;
    }

    public Date getEndMonth() {
        return endMonth;
    }

    public void setEndMonth(Date endMonth) {
        this.endMonth = endMonth;
    }

    public int getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(int monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public int getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(int totalCost) {
        this.totalCost = totalCost;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(Timestamp releasedAt) {
        this.releasedAt = releasedAt;
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
