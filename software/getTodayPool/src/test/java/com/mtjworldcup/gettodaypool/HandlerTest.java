package com.mtjworldcup.gettodaypool;

import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.dynamo.model.Match;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HandlerTest {

    private final MatchesDao mockMatchesDao = mock(MatchesDao.class);

    @Test
    void shouldReturn100_WhenOverallPoolIs100() {
        // Given
        Handler handler = new Handler(mockMatchesDao);
        when(mockMatchesDao.getTodayPool()).thenReturn(prepareOverallPool(new BigDecimal(100)));
        // When
        var response = handler.handleRequest(null, null);
        // Then
        Integer overallPool = Integer.valueOf(response.getBody());
        assertEquals(100, overallPool);
        assertEquals(200, response.getStatusCode());
    }

    @Test
    void shouldReturn200_WhenOverallPoolIs200() {
        // Given
        Handler handler = new Handler(mockMatchesDao);
        when(mockMatchesDao.getTodayPool()).thenReturn(prepareOverallPool(new BigDecimal(200)));
        // When
        var response = handler.handleRequest(null, null);
        // Then
        Integer overallPool = Integer.valueOf(response.getBody());
        assertEquals(200, overallPool);
        assertEquals(200, response.getStatusCode());
    }

    @Test
    void shouldReturn404_WhenOverallPoolIsNotAvailable() {
        // Given
        Handler handler = new Handler(mockMatchesDao);
        when(mockMatchesDao.getTodayPool()).thenThrow(new RuntimeException("Error"));
        // When
        var response = handler.handleRequest(null, null);
        // Then
        assertEquals("Nie udało się pobrać puli. Spróbuj ponownie później.", response.getBody());
        assertEquals(404, response.getStatusCode());
    }

    private Match prepareOverallPool(BigDecimal overallPool) {
        Match match = new Match();
        match.setPool(overallPool);
        return match;
    }

}