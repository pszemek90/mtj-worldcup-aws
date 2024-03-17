package com.mtjworldcup;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.model.Matches;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
        when(mockDao.getByDate(any())).thenReturn(List.of(new Match()));
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
        when(mockDao.getByDate(any())).thenReturn(List.of(new Match(), new Match()));
        Handler handler = new Handler(mockDao);
        var request = new APIGatewayProxyRequestEvent().withPathParameters(Map.of("date", "2023-10-28"));
        //when
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);
        //then
        Matches actualMatches = objectMapper.readValue(response.getBody(), Matches.class);
        assertEquals(2, actualMatches.getMatches().size());
    }
}