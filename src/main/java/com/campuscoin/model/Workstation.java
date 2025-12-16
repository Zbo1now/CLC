package com.campuscoin.model;

import java.sql.Timestamp;

public class Workstation {
    private int id;
    private String stationCode;
    private String location;
    private int monthlyRent;
    // 由查询计算出来的字段：当前自然月是否被占用
    private boolean occupied;
    private Timestamp createdAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStationCode() {
        return stationCode;
    }

    public void setStationCode(String stationCode) {
        this.stationCode = stationCode;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(int monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
