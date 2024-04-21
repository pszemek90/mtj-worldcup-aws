package com.mtjworldcup.getcurrentstatefromapi;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtjworldcup.common.model.MatchApiResponse;
import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.dynamo.model.MatchStatus;
import com.mtjworldcup.getcurrentstatefromapi.service.MatchStateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HandlerTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final MatchesDao mockMatchesDao = mock(MatchesDao.class);
    private final MatchStateService matchStateService = mock(MatchStateService.class);

    @Captor
    private ArgumentCaptor<Match> matchCaptor;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void shouldNotChangeMatchStatus_WhenMatchHasNotStarted() throws Exception {
        // given
        Match match = new Match();
        match.setMatchStatus(MatchStatus.SCHEDULED);
        match.setPrimaryId("1035522");
        when(mockMatchesDao.getByDate(LocalDate.now())).thenReturn(List.of(match));
        MatchApiResponse matchApiResponse = objectMapper.readValue(
                Files.readString(Path.of("src/test/resources/files/api-success-response.json")),
                MatchApiResponse.class);
        when(matchStateService.getCurrentState(List.of("1035522"))).thenReturn(matchApiResponse);
        Handler handler = new Handler(mockMatchesDao, matchStateService);

        // when
        handler.handleRequest(null, null);

        // then
        verify(mockMatchesDao).update(matchCaptor.capture());
        assertEquals(MatchStatus.SCHEDULED, matchCaptor.getValue().getMatchStatus());
    }

    @Test
    void shouldUpdateStatus_WhenMatchHasFinished() throws Exception {
        // given
        Match match = new Match();
        match.setMatchStatus(MatchStatus.SCHEDULED);
        match.setPrimaryId("1035522");
        when(mockMatchesDao.getByDate(LocalDate.now())).thenReturn(List.of(match));
        MatchApiResponse matchApiResponse = objectMapper.readValue(
                Files.readString(Path.of("src/test/resources/files/api-success-response-with-finished-match.json")),
                MatchApiResponse.class);
        when(matchStateService.getCurrentState(List.of("1035522"))).thenReturn(matchApiResponse);
        Handler handler = new Handler(mockMatchesDao, matchStateService);

        // when
        handler.handleRequest(null, null);

        // then
        verify(mockMatchesDao).update(matchCaptor.capture());
        assertEquals(MatchStatus.FINISHED, matchCaptor.getValue().getMatchStatus());
    }

    @Test
    void shouldNotThrowException_WhenMatchStateServiceFails() {
        // given
        Match match = new Match();
        match.setMatchStatus(MatchStatus.SCHEDULED);
        match.setPrimaryId("1035522");
        when(mockMatchesDao.getByDate(LocalDate.now())).thenReturn(List.of(match));
        when(matchStateService.getCurrentState(List.of("1035522"))).thenThrow(new RuntimeException("API is down"));
        Handler handler = new Handler(mockMatchesDao, matchStateService);

        // when, then
        APIGatewayProxyResponseEvent response = assertDoesNotThrow(() -> handler.handleRequest(null, null));
        assertEquals(500, response.getStatusCode());
    }
}