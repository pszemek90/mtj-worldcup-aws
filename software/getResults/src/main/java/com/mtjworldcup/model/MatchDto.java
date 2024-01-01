package com.mtjworldcup.model;

import java.time.LocalDate;

public record MatchDto(String homeTeam, Integer homeScore, Integer awayScore, String awayTeam,
                       LocalDate date, Integer correctTypings) {
}
