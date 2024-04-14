package com.mtjworldcup.getbydate.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record MatchDto(String matchId, LocalDate date, LocalTime startTime, String homeTeam, String awayTeam, Integer pool) {}
