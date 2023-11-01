package com.mtjworldcup.model;

import java.time.LocalDateTime;

public record MatchDto(Long matchId, LocalDateTime startTime, String homeTeam, String awayTeam) {}
