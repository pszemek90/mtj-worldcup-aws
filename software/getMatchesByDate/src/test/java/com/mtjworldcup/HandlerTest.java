package com.mtjworldcup;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mtjworldcup.dao.MatchesDao;
import com.mtjworldcup.model.MatchDto;
import com.mtjworldcup.model.Matches;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.time.Month.OCTOBER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
    void shouldReturnOneMatch_WhenOneMatchForADateAvailable() throws Exception {
        //given
        MatchesDao mockDao = mock(MatchesDao.class);
        List<MatchDto> matchesFromDate = new ArrayList<>();
        matchesFromDate.add(new MatchDto(123L, LocalDateTime.of(LocalDate.of(2023, OCTOBER, 28), LocalTime.of(15, 0)), "dummyHomeTeam", "dummyAwayTeam"));
        when(mockDao.getMatchesFromDatabase(any())).thenReturn(matchesFromDate);
        Handler handler = new Handler(mockDao);
        var request = new APIGatewayProxyRequestEvent().withPathParameters(Map.of("date", "2023-10-28"));
        //when
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);
        //then
        Matches actualMatches = objectMapper.readValue(response.getBody(), Matches.class);
        assertEquals(1, actualMatches.getMatches().size());
    }

    @Test
    void shouldReturnTwoMatches_WhenTwoMatchesForADateAvailable() throws Exception{
        //given
        MatchesDao mockDao = mock(MatchesDao.class);
        List<MatchDto> matchesFromDate = new ArrayList<>();
        matchesFromDate.add(new MatchDto(123L, LocalDateTime.of(LocalDate.of(2023, OCTOBER, 28), LocalTime.of(15, 0)), "dummyHomeTeam", "dummyAwayTeam"));
        matchesFromDate.add(new MatchDto(124L, LocalDateTime.of(LocalDate.of(2023, OCTOBER, 28), LocalTime.of(15, 0)), "dummyHomeTeam", "dummyAwayTeam"));
        when(mockDao.getMatchesFromDatabase(any())).thenReturn(matchesFromDate);
        Handler handler = new Handler(mockDao);
        var request = new APIGatewayProxyRequestEvent().withPathParameters(Map.of("date", "2023-10-28"));
        //when
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);
        //then
        Matches actualMatches = objectMapper.readValue(response.getBody(), Matches.class);
        assertEquals(2, actualMatches.getMatches().size());
    }
}