package com.mtjworldcup;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.cognito.exception.SignatureVerifierException;
import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.model.MatchDto;
import com.mtjworldcup.cognito.service.CognitoJwtVerifierService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HandlerTest {

    private CognitoJwtVerifierService cognitoJwtVerifierService = mock(CognitoJwtVerifierService.class);
    private MatchesDao matchesDao = mock(MatchesDao.class);
    private ObjectMapper objectMapper = new ObjectMapper();

    private Handler handler = new Handler(cognitoJwtVerifierService, matchesDao, objectMapper);

    @Test
    void shouldReturn403_WhenTokenNotVerified() throws Exception{
        //given
        when(cognitoJwtVerifierService.getSubject(any())).thenThrow(SignatureVerifierException.class);
        APIGatewayProxyRequestEvent input = new APIGatewayProxyRequestEvent().withHeaders(
                Map.of("Authorization", "Bearer someToken"));
        //when
        var response = handler.handleRequest(input, null);
        //then
        assertEquals(403, response.getStatusCode());
        verify(cognitoJwtVerifierService).getSubject("someToken");
    }

    @Test
    void shouldTryToSaveTypes_WhenTokenVerified() throws Exception{
        //given
        when(cognitoJwtVerifierService.getSubject(any())).thenReturn("someSubject");
        MatchDto[] types = {new MatchDto("match-123", 1, 1)};
        APIGatewayProxyRequestEvent input = new APIGatewayProxyRequestEvent()
                .withHeaders(Map.of("Authorization", "Bearer someToken"))
                .withBody(objectMapper.writeValueAsString(types));
        Match match123 = prepareMatch("match-123");
        when(matchesDao.getById("match-123")).thenReturn(match123);
        //when
        var response = handler.handleRequest(input, null);
        //then
        assertEquals(201, response.getStatusCode());
        verify(matchesDao).save(any());
    }

    @Test
    void shouldNotTryToSaveTypes_WhenNoTypesPassDateCriteria() throws Exception{
        //given
        when(cognitoJwtVerifierService.getSubject(any())).thenReturn("someSubject");
        MatchDto[] types = {new MatchDto("match-123", 1, 1)};
        APIGatewayProxyRequestEvent input = new APIGatewayProxyRequestEvent()
                .withHeaders(Map.of("Authorization", "Bearer someToken"))
                .withBody(objectMapper.writeValueAsString(types));
        Match match123 = prepareMatch("match-123");
        match123.setDate(LocalDate.now().minusDays(1));
        when(matchesDao.getById("match-123")).thenReturn(match123);
        //when
        var response = handler.handleRequest(input, null);
        //then
        verify(matchesDao, times(0)).save(any());
        assertEquals(204, response.getStatusCode());
    }

    private Match prepareMatch(String primaryId) {
        Match match = new Match();
        match.setPrimaryId(primaryId);
        match.setDate(LocalDate.now());
        match.setStartTime(LocalTime.now().plusHours(1));
        return match;
    }
}