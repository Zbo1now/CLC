package com.campuscoin.model.admin;

public class AdminDailyFlow {
    private String date;
    private Integer inflow;
    private Integer outflow;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Integer getInflow() {
        return inflow;
    }

    public void setInflow(Integer inflow) {
        this.inflow = inflow;
    }

    public Integer getOutflow() {
        return outflow;
    }

    public void setOutflow(Integer outflow) {
        this.outflow = outflow;
    }
}
