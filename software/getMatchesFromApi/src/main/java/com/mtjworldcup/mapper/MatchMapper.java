package com.mtjworldcup.mapper;

import com.mtjworldcup.model.Match;
import com.mtjworldcup.model.MatchDto;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

public class MatchMapper {
    public static Match mapToEntity(MatchDto dto) {
        Match match = new Match();
        match.setMatchId(dto.getFixture().getId());
        match.setStartTime(Instant.ofEpochSecond(dto.getFixture().getTimestamp())
                .atZone(ZoneId.of("Europe/Warsaw")).toLocalDateTime());
        match.setHomeTeam(dto.getTeams().getHome().getName());
        match.setAwayTeam(dto.getTeams().getAway().getName());
        match.setHomeScore(dto.getGoals().getHome());
        match.setAwayScore(dto.getGoals().getAway());
        return match;
    }

    public static List<Match> mapToEntity(List<MatchDto> dtos) {
        return dtos.stream().map(MatchMapper::mapToEntity).toList();
    }
}