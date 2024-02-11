package com.mtjworldcup.mapper;

import com.mtjworldcup.model.MatchDto;
import com.mtjworldcup.model.Match;
import com.mtjworldcup.model.RecordType;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

public class MatchMapper {

    private MatchMapper() {
        // private constructor to hide implicit public one
    }

    public static Match mapToEntity(MatchDto dto) {
        Match match = new Match();
        match.setPrimaryId("match-" + dto.getFixture().getId());
        match.setSecondaryId("match-" + dto.getFixture().getId());
        match.setDate(Instant.ofEpochSecond(dto.getFixture().getTimestamp())
                .atZone(ZoneId.of("Europe/Warsaw")).toLocalDate());
        match.setStartTime(Instant.ofEpochSecond(dto.getFixture().getTimestamp())
                .atZone(ZoneId.of("Europe/Warsaw")).toLocalTime());
        match.setHomeTeam(dto.getTeams().getHome().getName());
        match.setAwayTeam(dto.getTeams().getAway().getName());
        match.setHomeScore(dto.getGoals().getHome());
        match.setAwayScore(dto.getGoals().getAway());
        match.setRecordType(RecordType.MATCH);
        return match;
    }

    public static List<Match> mapToEntity(List<MatchDto> dtos) {
        return dtos.stream().map(MatchMapper::mapToEntity).toList();
    }
}
