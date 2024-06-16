package com.mtjworldcup.getbydate.mapper;

import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.getbydate.model.MatchDto;

import java.util.List;

public class MatchMapper {

    private MatchMapper(){}

    public static MatchDto mapToDto(Match entity) {
        return new MatchDto(entity.getPrimaryId(), entity.getDate(), entity.getStartTime(), entity.getHomeTeam(),
                entity.getAwayTeam(), entity.getPool(), entity.getDisplayPool());
    }

    public static List<MatchDto> mapToDto(List<Match> entities) {
        return entities.stream().map(MatchMapper::mapToDto).toList();
    }
}
