package com.mtjworldcup.mapper;

import com.amazonaws.util.CollectionUtils;
import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.model.Typing;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class TypingMapper {

    private TypingMapper() {
        // hide implicit public constructor
    }

    public static Map<LocalDate, List<Typing>> mapToDto(List<Match> records) {
        if (CollectionUtils.isNullOrEmpty(records)) {
            return Map.of();
        }
        return records.stream()
                .collect(Collectors.groupingBy(
                        Match::getDate,
                        () -> new TreeMap<>(Comparator.reverseOrder()),
                        Collectors.mapping(TypingMapper::mapToDto, Collectors.toList())
                ));
    }

    private static Typing mapToDto(Match match) {
        return new Typing(match.getHomeTeam(), match.getHomeScore() + " - " + match.getAwayScore(),
                match.getAwayTeam(), match.getTypingStatus());
    }
}
