package com.mtjworldcup.model;

import java.time.LocalTime;

public record MatchDto(String matchId, LocalTime startTime, String homeTeam, String awayTeam) {}
