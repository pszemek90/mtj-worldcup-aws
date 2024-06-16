package com.mtjworldcup.getbydate.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record MatchDto(String matchId, LocalDate date, LocalTime startTime, String homeTeam, String awayTeam,
                       BigDecimal pool, BigDecimal displayPool) {}
