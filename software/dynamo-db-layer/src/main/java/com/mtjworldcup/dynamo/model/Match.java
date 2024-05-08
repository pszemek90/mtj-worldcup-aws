package com.mtjworldcup.dynamo.model;

import com.mtjworldcup.common.model.TypingStatus;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

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
    private int correctTypings;
    private TypingStatus typingStatus;
    private MatchStatus matchStatus;
    private RecordType recordType;
    private BigDecimal pool;
    private String fcmToken;
    private String endpointArn;

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
    public int getCorrectTypings() {
        return correctTypings;
    }

    public void setCorrectTypings(int correctTypings) {
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

    @DynamoDbAttribute("pool")
    public BigDecimal getPool() {
        return pool;
    }

    public void setPool(BigDecimal pool) {
        this.pool = pool;
    }

    @DynamoDbAttribute("fcm_token")
    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    @DynamoDbAttribute("endpoint_arn")
    public String getEndpointArn() {
        return endpointArn;
    }

    public void setEndpointArn(String endpointArn) {
        this.endpointArn = endpointArn;
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
                ", fcmToken='" + fcmToken + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Match match = (Match) o;
        return correctTypings == match.correctTypings && Objects.equals(primaryId, match.primaryId) && Objects.equals(secondaryId, match.secondaryId) && Objects.equals(date, match.date) && Objects.equals(startTime, match.startTime) && Objects.equals(homeTeam, match.homeTeam) && Objects.equals(awayTeam, match.awayTeam) && Objects.equals(homeScore, match.homeScore) && Objects.equals(awayScore, match.awayScore) && typingStatus == match.typingStatus && matchStatus == match.matchStatus && recordType == match.recordType && Objects.equals(pool, match.pool) && Objects.equals(fcmToken, match.fcmToken) && Objects.equals(endpointArn, match.endpointArn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(primaryId, secondaryId, date, startTime, homeTeam, awayTeam, homeScore, awayScore, correctTypings, typingStatus, matchStatus, recordType, pool, fcmToken, endpointArn);
    }
}
