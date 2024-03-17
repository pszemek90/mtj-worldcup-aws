package com.mtjworldcup.mapper;

import com.amazonaws.util.CollectionUtils;
import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.model.Typings;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class TypingMapper {

    private TypingMapper() {
        // hide implicit public constructor
    }

    public static Typings mapToDto(List<Match> records) {
        if(CollectionUtils.isNullOrEmpty(records)){
            return new Typings(Map.of());
        }
        return new Typings(records.stream()
                .collect(Collectors.groupingBy(
                        Match::getDate,
                        () -> new TreeMap<>(Comparator.reverseOrder()),
                        Collectors.mapping(TypingMapper::mapToDto, Collectors.toList())
                )));
    }

    private static Typings.Typing mapToDto(Match match) {
        return new Typings.Typing(match.getHomeTeam() + " - " + match.getAwayTeam(), match.getTypingStatus());
    }
}
