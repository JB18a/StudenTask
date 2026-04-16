package com.example.ccc.entity;

import java.util.List;

public class CheckinVO {

    private Boolean checkedToday;
    private Integer continuousDays;
    private Integer totalDays;
    private Integer todayPoints;
    private Integer bonusPoints;
    private List<Integer> monthlyCheckins;

    public CheckinVO() {}

    public Boolean getCheckedToday() { return checkedToday; }
    public void setCheckedToday(Boolean checkedToday) { this.checkedToday = checkedToday; }

    public Integer getContinuousDays() { return continuousDays; }
    public void setContinuousDays(Integer continuousDays) { this.continuousDays = continuousDays; }

    public Integer getTotalDays() { return totalDays; }
    public void setTotalDays(Integer totalDays) { this.totalDays = totalDays; }

    public Integer getTodayPoints() { return todayPoints; }
    public void setTodayPoints(Integer todayPoints) { this.todayPoints = todayPoints; }

    public Integer getBonusPoints() { return bonusPoints; }
    public void setBonusPoints(Integer bonusPoints) { this.bonusPoints = bonusPoints; }

    public List<Integer> getMonthlyCheckins() { return monthlyCheckins; }
    public void setMonthlyCheckins(List<Integer> monthlyCheckins) { this.monthlyCheckins = monthlyCheckins; }
}
