package com.mtjworldcup.getalltypings.mapper;

import com.mtjworldcup.common.model.TypingStatus;
import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.getalltypings.model.TypingDto;

import java.util.List;

public class TypingMapper {

    private TypingMapper() {
        // Utility class
    }

    public static TypingDto toTypingDto(Match match) {
        return new TypingDto(match.getDate(),
                match.getHomeTeam() + " - " + match.getAwayTeam(),
                match.getSecondaryId(),
                match.getHomeScore() + " - " + match.getAwayScore(),
                match.getTypingStatus() == TypingStatus.CORRECT);
    }

    public static List<TypingDto> toTypingDto(List<Match> matches) {
        return matches.stream()
                .map(TypingMapper::toTypingDto)
                .toList();
    }
}
