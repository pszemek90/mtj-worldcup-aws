package com.mtjworldcup;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mtjworldcup.cognito.exception.SignatureVerifierException;
import com.mtjworldcup.cognito.service.CognitoJwtVerifierService;
import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.model.Typing;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class HandlerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final MatchesDao matchesDao = mock(MatchesDao.class);
    private final CognitoJwtVerifierService cognitoJwtVerifierService = mock(CognitoJwtVerifierService.class);
    private final ObjectMapper objectMapper = spy(ObjectMapper.class);
    private Handler handler;

    @BeforeAll
    static void setUpOnce() {
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
    }

    @BeforeEach
    void setUp() {
        handler = new Handler(objectMapper, cognitoJwtVerifierService, matchesDao);
    }

    @Test
    void shouldReturnNoTypes_WhenNoTypesAreAvailable() throws Exception {
        //given
        String token = "Bearer testToken";
        var request = new APIGatewayProxyRequestEvent().withHeaders(Map.of("Authorization", token));
        //when
        var response = handler.handleRequest(request, null);
        //then
        var typings = OBJECT_MAPPER.readValue(response.getBody(), new TypeReference<Map<LocalDate, List<Typing>>>() {
        });
        assertEquals(0, typings.size());
        assertEquals(200, response.getStatusCode());
    }

    @Test
    void shouldReturnOneTyping_WhenOneTypingIsAvailable() throws Exception {
        //given
        String token = "Bearer testToken";
        var request = new APIGatewayProxyRequestEvent().withHeaders(Map.of("Authorization", token));
        String testUserId = "testUserId";
        when(cognitoJwtVerifierService.getSubject("testToken")).thenReturn(testUserId);
        when(matchesDao.getTypings(testUserId)).thenReturn(List.of(prepareMatch()));
        //when
        var response = handler.handleRequest(request, null);
        //then
        var typings = OBJECT_MAPPER.readValue(response.getBody(), new TypeReference<Map<LocalDate, List<Typing>>>() {
        });
        assertEquals(1, typings.size());
        assertEquals(200, response.getStatusCode());
    }

    @Test
    void shouldReturn401_WhenSignatureExceptionIsThrown() throws Exception {
        //given
        String token = "Bearer testToken";
        var request = new APIGatewayProxyRequestEvent().withHeaders(Map.of("Authorization", token));
        when(cognitoJwtVerifierService.getSubject("testToken")).thenThrow(new SignatureVerifierException("No user found for token"));
        //when
        var response = handler.handleRequest(request, null);
        //then
        assertEquals(401, response.getStatusCode());
        verifyNoInteractions(matchesDao);
        verifyNoInteractions(objectMapper);
    }

    @Test
    void shouldReturn500_WhenJsonProcessingExceptionIsThrown() throws Exception {
        //given
        String token = "Bearer testToken";
        var request = new APIGatewayProxyRequestEvent().withHeaders(Map.of("Authorization", token));
        String testUserId = "testUserId";
        when(cognitoJwtVerifierService.getSubject(token)).thenReturn(testUserId);
        when(matchesDao.getTypings(testUserId)).thenReturn(List.of(prepareMatch()));
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("testJsonProcessingException") {
        });
        //when
        var response = handler.handleRequest(request, null);
        //then
        assertEquals(500, response.getStatusCode());
    }

    private Match prepareMatch() {
        Match match = new Match();
        match.setHomeTeam("testHomeTeam");
        match.setAwayTeam("testAwayTeam");
        match.setHomeScore(10);
        match.setAwayScore(20);
        match.setDate(LocalDate.of(2024, Month.MARCH, 13));
        return match;
    }
}