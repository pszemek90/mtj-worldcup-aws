package com.mtjworldcup.getuserprofile;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.mtjworldcup.cognito.exception.SignatureVerifierException;
import com.mtjworldcup.cognito.service.CognitoJwtVerifierService;
import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.dynamo.model.Match;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HandlerTest {

    private final CognitoJwtVerifierService cognitoJwtVerifierService = mock(CognitoJwtVerifierService.class);
    private final MatchesDao matchesDao = mock(MatchesDao.class);

    @Test
    void shouldReturnUsersBalance_WhenUserBalanceIs100() throws Exception {
        //given
        Handler handler = new Handler(cognitoJwtVerifierService, matchesDao);
        when(cognitoJwtVerifierService.checkUser("testToken")).thenReturn("testUserId");
        Match user = new Match();
        user.setPool(100);
        when(matchesDao.getById("testUserId")).thenReturn(user);
        //when
        var response = handler.handleRequest(
                new APIGatewayProxyRequestEvent().withHeaders(
                        Map.ofEntries(Map.entry("Authorization", "Bearer testToken"))),
                null);
        //then
        String userBalance = response.getBody();
        assertEquals(100, Integer.parseInt(userBalance));
        assertEquals(200, response.getStatusCode());
    }

    @Test
    void shouldReturnUsersBalance_WhenUserBalanceIs200() throws Exception {
        //given
        Handler handler = new Handler(cognitoJwtVerifierService, matchesDao);
        when(cognitoJwtVerifierService.checkUser("testToken")).thenReturn("testUserId");
        Match user = new Match();
        user.setPool(200);
        when(matchesDao.getById("testUserId")).thenReturn(user);
        //when
        var response = handler.handleRequest(
                new APIGatewayProxyRequestEvent().withHeaders(
                        Map.ofEntries(Map.entry("Authorization", "Bearer testToken"))),
                null);
        //then
        String userBalance = response.getBody();
        assertEquals(200, Integer.parseInt(userBalance));
        assertEquals(200, response.getStatusCode());
    }

    @Test
    void shouldReturnUnauthorized_WhenTokenIsInvalid() throws Exception {
        //given
        Handler handler = new Handler(cognitoJwtVerifierService, matchesDao);
        when(cognitoJwtVerifierService.checkUser("testToken")).thenThrow(new SignatureVerifierException("Invalid token"));
        //when
        var response = handler.handleRequest(
                new APIGatewayProxyRequestEvent().withHeaders(
                        Map.ofEntries(Map.entry("Authorization", "Bearer testToken"))),
                null);
        //then
        assertEquals(401, response.getStatusCode());
        assertEquals("Unauthorized", response.getBody());
    }

    @Test
    void shouldReturnUserNotFound_WhenUserIsNotFound() throws Exception {
        //given
        Handler handler = new Handler(cognitoJwtVerifierService, matchesDao);
        when(cognitoJwtVerifierService.checkUser("testToken")).thenReturn("testUserId");
        when(matchesDao.getById("testUserId")).thenReturn(null);
        //when
        var response = handler.handleRequest(
                new APIGatewayProxyRequestEvent().withHeaders(
                        Map.ofEntries(Map.entry("Authorization", "Bearer testToken"))),
                null);
        //then
        assertEquals(404, response.getStatusCode());
        assertEquals("User not found", response.getBody());
    }

    @Test
    void shouldReturnNotFound_WhenUserPoolIsNull() throws Exception {
        //given
        Handler handler = new Handler(cognitoJwtVerifierService, matchesDao);
        when(cognitoJwtVerifierService.checkUser("testToken")).thenReturn("testUserId");
        Match user = new Match();
        user.setPool(null);
        when(matchesDao.getById("testUserId")).thenReturn(user);
        //when
        var response = handler.handleRequest(
                new APIGatewayProxyRequestEvent().withHeaders(
                        Map.ofEntries(Map.entry("Authorization", "Bearer testToken"))),
                null);
        //then
        assertEquals(404, response.getStatusCode());
        assertEquals("User not found", response.getBody());
    }
}