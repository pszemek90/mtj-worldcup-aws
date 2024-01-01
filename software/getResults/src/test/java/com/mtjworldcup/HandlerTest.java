package com.mtjworldcup;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mtjworldcup.dao.MatchesDao;
import com.mtjworldcup.model.Match;
import com.mtjworldcup.model.Matches;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

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
    void shouldReturnOneMatch_WhenOneMatchIsFinished() throws Exception {
        // given
        MatchesDao mockDao = mock(MatchesDao.class);
        Handler handler = new Handler(mockDao);
        when(mockDao.getFinishedMatches()).thenReturn(List.of(new Match()));
        var request = new APIGatewayProxyRequestEvent();
        //when
        var response = handler.handleRequest(request, null);
        //then
        Matches matches = objectMapper.readValue(response.getBody(), Matches.class);
        assertEquals(1, matches.matches().size());
    }
}