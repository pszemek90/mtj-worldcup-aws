package com.mtjworldcup.getresults.mapper;

import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.getresults.model.MatchDto;

import java.util.List;

public class MatchMapper {

    private MatchMapper() {}

    public static MatchDto mapToDto(Match entity) {
        return new MatchDto(entity.getHomeTeam(), entity.getHomeScore() + " - " + entity.getAwayScore(), entity.getAwayTeam(), entity.getCorrectTypings());
    }

    public static List<MatchDto> mapToDto(List<Match> entities) {
        return entities.stream().map(MatchMapper::mapToDto).toList();
    }
}
