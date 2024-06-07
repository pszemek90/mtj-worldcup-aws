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
import java.util.Map;
import java.util.Optional;

public class MatchMapper {

    private MatchMapper() {
        // private constructor to hide implicit public one
    }

    private static final Map<String, String> countries = Map.ofEntries(
            Map.entry("Germany", "Niemcy"),
            Map.entry("Scotland", "Szkocja"),
            Map.entry("Hungary", "Węgry"),
            Map.entry("Switzerland", "Szwajcaria"),
            Map.entry("Spain", "Hiszpania"),
            Map.entry("Croatia", "Chorwacja"),
            Map.entry("Italy", "Włochy"),
            Map.entry("Albania", "Albania"),
            Map.entry("Slovenia", "Słowenia"),
            Map.entry("Denmark", "Dania"),
            Map.entry("Serbia", "Serbia"),
            Map.entry("England", "Anglia"),
            Map.entry("Austria", "Austria"),
            Map.entry("France", "Francja"),
            Map.entry("Belgium", "Belgia"),
            Map.entry("Slovakia", "Słowacja"),
            Map.entry("Portugal", "Portugalia"),
            Map.entry("Czech Republic", "Czechy"),
            Map.entry("Romania", "Rumunia"),
            Map.entry("Turkey", "Turcja"),
            Map.entry("Netherlands", "Holandia"),
            Map.entry("Poland", "Polska"),
            Map.entry("Ukraine", "Ukraina"),
            Map.entry("Georgia", "Gruzja")
    );

    public static MatchDto translateCountries(MatchDto dto) {
        Optional.of(dto)
                .map(MatchDto::getTeams)
                .map(MatchDto.Teams::getHome)
                .map(MatchDto.Teams.Team::getName)
                .map(countries::get)
                .ifPresent(dto.getTeams().getHome()::setName);
        Optional.of(dto)
                .map(MatchDto::getTeams)
                .map(MatchDto.Teams::getAway)
                .map(MatchDto.Teams.Team::getName)
                .map(countries::get)
                .ifPresent(dto.getTeams().getAway()::setName);
        return dto;
    }

    public static List<MatchDto> translateCountries(List<MatchDto> dtos) {
        return dtos.stream().map(MatchMapper::translateCountries).toList();
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
