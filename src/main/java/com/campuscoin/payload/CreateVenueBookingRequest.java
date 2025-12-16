package com.campuscoin.payload;

public class CreateVenueBookingRequest {
    private String date;      // yyyy-MM-dd
    private String startTime; // HH:mm
    private String endTime;   // HH:mm

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}
