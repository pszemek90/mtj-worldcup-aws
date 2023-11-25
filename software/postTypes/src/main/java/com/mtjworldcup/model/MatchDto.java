package com.mtjworldcup.model;

public class MatchDto {

    public MatchDto() {}

    public MatchDto(String primaryId, Integer homeScore, Integer awayScore) {
        this.primaryId = primaryId;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
    }

    private String primaryId;
    private Integer homeScore;
    private Integer awayScore;

    public String getPrimaryId() {
        return primaryId;
    }

    public void setPrimaryId(String primaryId) {
        this.primaryId = primaryId;
    }

    public Integer getHomeScore() {
        return homeScore;
    }

    public void setHomeScore(Integer homeScore) {
        this.homeScore = homeScore;
    }

    public Integer getAwayScore() {
        return awayScore;
    }

    public void setAwayScore(Integer awayScore) {
        this.awayScore = awayScore;
    }
}
