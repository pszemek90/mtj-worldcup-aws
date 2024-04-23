package com.mtjworldcup.getfromapi.mapper;

import com.mtjworldcup.common.model.MatchDto;
import com.mtjworldcup.dynamo.model.MatchStatus;
import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.dynamo.model.RecordType;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

public class MatchMapper {

    private MatchMapper() {
        // private constructor to hide implicit public one
    }

    public static Match mapToEntity(MatchDto dto) {
        Match match = new Match();
        match.setPrimaryId(dto.getFixture().getId().toString());
        match.setSecondaryId(dto.getFixture().getId().toString());
        match.setDate(Instant.ofEpochSecond(dto.getFixture().getTimestamp())
                .atZone(ZoneId.of("Europe/Warsaw")).toLocalDate());
        match.setStartTime(Instant.ofEpochSecond(dto.getFixture().getTimestamp())
                .atZone(ZoneId.of("Europe/Warsaw")).toLocalTime());
        match.setHomeTeam(dto.getTeams().getHome().getName());
        match.setAwayTeam(dto.getTeams().getAway().getName());
        match.setRecordType(RecordType.MATCH);
        match.setMatchStatus(MatchStatus.SCHEDULED);
        match.setPool(new BigDecimal(0, new MathContext(2)));
        return match;
    }

    public static List<Match> mapToEntity(List<MatchDto> dtos) {
        return dtos.stream().map(MatchMapper::mapToEntity).toList();
    }
}
