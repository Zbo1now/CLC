package com.campuscoin.model;

import java.sql.Date;
import java.sql.Timestamp;

public class Checkin {
    private int id;
    private int teamId;
    private Date checkinDate;
    private int coinsEarned;
    private Timestamp createdAt;

    public Checkin() {
    }

    public Checkin(int teamId, Date checkinDate, int coinsEarned) {
        this.teamId = teamId;
        this.checkinDate = checkinDate;
        this.coinsEarned = coinsEarned;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTeamId() {
        return teamId;
    }

    public void setTeamId(int teamId) {
        this.teamId = teamId;
    }

    public Date getCheckinDate() {
        return checkinDate;
    }

    public void setCheckinDate(Date checkinDate) {
        this.checkinDate = checkinDate;
    }

    public int getCoinsEarned() {
        return coinsEarned;
    }

    public void setCoinsEarned(int coinsEarned) {
        this.coinsEarned = coinsEarned;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
