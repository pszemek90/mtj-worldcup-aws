package com.mtjworldcup.dynamo.model;

import com.mtjworldcup.common.model.TypingStatus;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.LocalDate;
import java.time.LocalTime;

@DynamoDbBean
public class Match {
    private String primaryId;
    private String secondaryId;
    private LocalDate date;
    private LocalTime startTime;
    private String homeTeam;
    private String awayTeam;
    private Integer homeScore;
    private Integer awayScore;
    private Integer correctTypings;
    private TypingStatus typingStatus;
    private MatchStatus matchStatus;
    private RecordType recordType;
    private int pool;

    @DynamoDbSecondarySortKey(indexNames = {"getBySecondaryId"})
    @DynamoDbPartitionKey
    @DynamoDbAttribute("primary_id")
    public String getPrimaryId() {
        return primaryId;
    }

    public void setPrimaryId(String primaryId) {
        this.primaryId = primaryId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"getBySecondaryId"})
    @DynamoDbAttribute("secondary_id")
    @DynamoDbSortKey
    public String getSecondaryId() {
        return secondaryId;
    }

    public void setSecondaryId(String secondaryId) {
        this.secondaryId = secondaryId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"getByDate"})
    @DynamoDbAttribute("date")
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    @DynamoDbAttribute("start_time")
    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    @DynamoDbAttribute("home_team")
    public String getHomeTeam() {
        return homeTeam;
    }

    public void setHomeTeam(String homeTeam) {
        this.homeTeam = homeTeam;
    }

    @DynamoDbAttribute("away_team")
    public String getAwayTeam() {
        return awayTeam;
    }

    public void setAwayTeam(String awayTeam) {
        this.awayTeam = awayTeam;
    }

    @DynamoDbAttribute("home_score")
    public Integer getHomeScore() {
        return homeScore;
    }

    public void setHomeScore(Integer homeScore) {
        this.homeScore = homeScore;
    }

    @DynamoDbAttribute("away_score")
    public Integer getAwayScore() {
        return awayScore;
    }

    public void setAwayScore(Integer awayScore) {
        this.awayScore = awayScore;
    }

    @DynamoDbAttribute("correct_typings")
    public Integer getCorrectTypings() {
        return correctTypings;
    }

    public void setCorrectTypings(Integer correctTypings) {
        this.correctTypings = correctTypings;
    }

    @DynamoDbAttribute("typing_status")
    public TypingStatus getTypingStatus() {
        return typingStatus;
    }

    public void setTypingStatus(TypingStatus typingStatus) {
        this.typingStatus = typingStatus;
    }

    @DynamoDbAttribute("match_status")
    public MatchStatus getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(MatchStatus matchStatus) {
        this.matchStatus = matchStatus;
    }

    @DynamoDbAttribute("record_type")
    @DynamoDbSecondaryPartitionKey(indexNames = {"getByRecordType"})
    public RecordType getRecordType() {
        return recordType;
    }

    public void setRecordType(RecordType recordType) {
        this.recordType = recordType;
    }

    public int getPool() {
        return pool;
    }

    public void setPool(int pool) {
        this.pool = pool;
    }

    @Override
    public String toString() {
        return "Match{" +
                "primaryId='" + primaryId + '\'' +
                ", secondaryId='" + secondaryId + '\'' +
                ", date=" + date +
                ", startTime=" + startTime +
                ", homeTeam='" + homeTeam + '\'' +
                ", awayTeam='" + awayTeam + '\'' +
                ", homeScore=" + homeScore +
                ", awayScore=" + awayScore +
                ", correctTypings=" + correctTypings +
                ", typingStatus=" + typingStatus +
                ", matchStatus=" + matchStatus +
                ", recordType=" + recordType +
                ", pool=" + pool +
                '}';
    }
}
