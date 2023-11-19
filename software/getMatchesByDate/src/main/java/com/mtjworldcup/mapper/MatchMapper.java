package com.mtjworldcup.mapper;

import com.mtjworldcup.model.Match;
import com.mtjworldcup.model.MatchDto;

public class MatchMapper {
    public static MatchDto mapToDto(Match entity) {
        return new MatchDto(entity.getPrimaryId(), entity.getStartTime(), entity.getHomeTeam(), entity.getAwayTeam());
    }
}
