package com.mtjworldcup;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mtjworldcup.dao.MatchesDao;
import com.mtjworldcup.model.Match;
import com.mtjworldcup.model.MatchDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HandlerTest {

    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void shouldReturnOneDateKey_WhenOneMatchIsFinished() throws Exception {
        // given
        MatchesDao mockDao = mock(MatchesDao.class);
        Handler handler = new Handler(mockDao);
        Match match = prepareMatch();
        when(mockDao.getFinishedMatches()).thenReturn(List.of(match));
        var request = new APIGatewayProxyRequestEvent();
        //when
        var response = handler.handleRequest(request, null);
        //then
        var mapType = new TypeReference<Map<LocalDate, List<MatchDto>>>() {};
        var matchesMap = objectMapper.readValue(response.getBody(), mapType);
        assertEquals(1, matchesMap.size());
    }

    @Test
    void shouldReturnTwoMatchesUnderOneDate_WhenTwoMatchesFromTheSameDateAreFinished() throws Exception {
        // given
        MatchesDao mockDao = mock(MatchesDao.class);
        Handler handler = new Handler(mockDao);
        Match match = prepareMatch(LocalDate.of(2024, 1, 5));
        Match match2 = prepareMatch(LocalDate.of(2024, 1, 5));
        when(mockDao.getFinishedMatches()).thenReturn(List.of(match, match2));
        var request = new APIGatewayProxyRequestEvent();
        //when
        var response = handler.handleRequest(request, null);
        //then
        var mapType = new TypeReference<Map<LocalDate, List<MatchDto>>>() {};
        var matchesMap = objectMapper.readValue(response.getBody(), mapType);
        assertEquals(1, matchesMap.size());
        List<MatchDto> matchDtos = matchesMap.get(LocalDate.of(2024, 1, 5));
        assertEquals(2, matchDtos.size());
    }

    @Test
    void shouldReturnTwoMatchesUnderTwoDates_WhenTwoMatchesFromDifferentDatesAreFinished() throws Exception {
        // given
        MatchesDao mockDao = mock(MatchesDao.class);
        Handler handler = new Handler(mockDao);
        Match match = prepareMatch(LocalDate.of(2024, 1, 5));
        Match match2 = prepareMatch(LocalDate.of(2024, 1, 6));
        when(mockDao.getFinishedMatches()).thenReturn(List.of(match, match2));
        var request = new APIGatewayProxyRequestEvent();
        //when
        var response = handler.handleRequest(request, null);
        //then
        var mapType = new TypeReference<Map<LocalDate, List<MatchDto>>>() {};
        var matchesMap = objectMapper.readValue(response.getBody(), mapType);
        assertEquals(2, matchesMap.size());
        List<MatchDto> matchDtos = matchesMap.get(LocalDate.of(2024, 1, 5));
        assertEquals(1, matchDtos.size());
    }

    private Match prepareMatch() {
        var match = new Match();
        match.setDate(LocalDate.of(2024, 1, 5));
        match.setHomeTeam("Home");
        match.setAwayTeam("Away");
        match.setHomeScore(1);
        match.setAwayScore(2);
        match.setCorrectTypings(1);
        return match;
    }

    private Match prepareMatch(LocalDate date) {
        Match match = prepareMatch();
        match.setDate(date);
        return match;
    }
}