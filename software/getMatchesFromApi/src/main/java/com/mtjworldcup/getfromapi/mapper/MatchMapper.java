package com.mtjworldcup.getfromapi.mapper;

import com.mtjworldcup.dynamo.model.MatchStatus;
import com.mtjworldcup.getfromapi.model.MatchDto;
import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.dynamo.model.RecordType;

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
        match.setRecordType(RecordType.MATCH);
        match.setMatchStatus(MatchStatus.SCHEDULED);
        match.setPool(0);
        return match;
    }

    public static List<Match> mapToEntity(List<MatchDto> dtos) {
        return dtos.stream().map(MatchMapper::mapToEntity).toList();
    }
}
