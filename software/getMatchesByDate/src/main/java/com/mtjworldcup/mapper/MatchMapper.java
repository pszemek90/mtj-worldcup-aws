package com.mtjworldcup.mapper;

import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.model.MatchDto;

import java.util.List;

public class MatchMapper {

    private MatchMapper(){}

    public static MatchDto mapToDto(Match entity) {
        return new MatchDto(entity.getPrimaryId(), entity.getStartTime(), entity.getHomeTeam(), entity.getAwayTeam());
    }

    public static List<MatchDto> mapToDto(List<Match> entities) {
        return entities.stream().map(MatchMapper::mapToDto).toList();
    }
}
