package com.mtjworldcup.getuserhistory.mapper;

import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.getuserhistory.model.MessageDto;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class MessageMapper {
    private MessageMapper() {}

    public static MessageDto mapToDto(Match entity) {
        return new MessageDto(
                Optional.of(entity)
                        .map(Match::getDate)
                        .map(date -> date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        .orElse("-"),
                "Wygrana: " + entity.getHomeTeam() + " - " + entity.getAwayTeam(),
                Optional.of(entity)
                        .map(Match::getPool)
                        .map(BigDecimal::toString)
                        .orElse("-")
        );
    }

    public static List<MessageDto> mapToDto(List<Match> entities) {
        return Optional.ofNullable(entities)
                .stream()
                .flatMap(List::stream)
                .map(MessageMapper::mapToDto)
                .toList();
    }
}
