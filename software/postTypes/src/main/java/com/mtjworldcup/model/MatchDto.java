package com.mtjworldcup.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchDto {

    public MatchDto() {}

    public MatchDto(String matchId, Integer homeScore, Integer awayScore) {
        this.matchId = matchId;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
    }

    private String matchId;
    private Integer homeScore;
    private Integer awayScore;

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
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
