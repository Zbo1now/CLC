package com.campuscoin.model;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class DeviceBooking {
    private int id;
    private int deviceId;
    private int teamId;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private LocalDateTime actualStartAt;
    private LocalDateTime actualEndAt;
    private String status;
    private int billedCost;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public int getTeamId() {
        return teamId;
    }

    public void setTeamId(int teamId) {
        this.teamId = teamId;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public LocalDateTime getActualStartAt() {
        return actualStartAt;
    }

    public void setActualStartAt(LocalDateTime actualStartAt) {
        this.actualStartAt = actualStartAt;
    }

    public LocalDateTime getActualEndAt() {
        return actualEndAt;
    }

    public void setActualEndAt(LocalDateTime actualEndAt) {
        this.actualEndAt = actualEndAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getBilledCost() {
        return billedCost;
    }

    public void setBilledCost(int billedCost) {
        this.billedCost = billedCost;
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
