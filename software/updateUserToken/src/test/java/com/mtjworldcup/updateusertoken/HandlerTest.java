package com.mtjworldcup.updateusertoken;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtjworldcup.cognito.exception.SignatureVerifierException;
import com.mtjworldcup.cognito.service.CognitoJwtVerifierService;
import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.updateusertoken.model.FCMToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HandlerTest {

  private final CognitoJwtVerifierService mockCognitoJwtVerifierService =
      mock(CognitoJwtVerifierService.class);
  private final MatchesDao mockMatchesDao = mock(MatchesDao.class);
  private final ObjectMapper spyObjectMapper = spy(ObjectMapper.class);

  private Handler handler;

  @BeforeEach
  void setUp() {
    handler = new Handler(mockCognitoJwtVerifierService, mockMatchesDao, spyObjectMapper);
  }

  @Test
  void shouldTryToUpdateUser_WhenValidDataPassed() throws Exception {
    // given
    APIGatewayProxyRequestEvent input =
        new APIGatewayProxyRequestEvent()
            .withBody("{\"token\":\"testToken\"}")
            .withHeaders(java.util.Map.of("Authorization", "Bearer testAuthToken"));
    when(mockCognitoJwtVerifierService.checkUser("testAuthToken")).thenReturn("testUserId");
    Match user = new Match();
    user.setPrimaryId("testUserId");
    when(mockMatchesDao.getById("testUserId")).thenReturn(user);
    // when
    APIGatewayProxyResponseEvent response = handler.handleRequest(input, null);
    // then
    assertNotNull(response);
    assertEquals(204, response.getStatusCode());
    Match expectedUser = new Match();
    expectedUser.setPrimaryId("testUserId");
    expectedUser.setFcmToken("testToken");
    verify(mockMatchesDao).update(expectedUser);
  }

  @Test
  void shouldReturn401_WhenSignatureVerificationFailed() throws Exception {
    // given
    APIGatewayProxyRequestEvent input =
        new APIGatewayProxyRequestEvent()
            .withBody("{\"token\":\"testToken\"}")
            .withHeaders(java.util.Map.of("Authorization", "Bearer testAuthToken"));
    when(mockCognitoJwtVerifierService.checkUser("testAuthToken"))
        .thenThrow(new SignatureVerifierException("user not found"));
    // when
    APIGatewayProxyResponseEvent response = handler.handleRequest(input, null);
    // then
    assertNotNull(response);
    assertEquals(401, response.getStatusCode());
  }

  @Test
  void shouldReturn400_WhenJsonMappingException() throws Exception {
    // given
    APIGatewayProxyRequestEvent input =
        new APIGatewayProxyRequestEvent()
            .withBody("{\"token\":\"testToken\"}")
            .withHeaders(java.util.Map.of("Authorization", "Bearer testAuthToken"));
    when(mockCognitoJwtVerifierService.checkUser("testAuthToken")).thenReturn("testUserId");
    Match user = new Match();
    user.setPrimaryId("testUserId");
    when(mockMatchesDao.getById("testUserId")).thenReturn(user);
    when(spyObjectMapper.readValue("{\"token\":\"testToken\"}", FCMToken.class))
        .thenThrow(new JsonMappingException("json mapping exception"));
    // when
    APIGatewayProxyResponseEvent response = handler.handleRequest(input, null);
    // then
    assertNotNull(response);
    assertEquals(400, response.getStatusCode());
  }

  @Test
  void shouldReturn400_WhenJsonProcessingException() throws Exception {
    // given
    APIGatewayProxyRequestEvent input =
        new APIGatewayProxyRequestEvent()
            .withBody("{\"token\":\"testToken\"}")
            .withHeaders(java.util.Map.of("Authorization", "Bearer testAuthToken"));
    when(mockCognitoJwtVerifierService.checkUser("testAuthToken")).thenReturn("testUserId");
    Match user = new Match();
    user.setPrimaryId("testUserId");
    when(mockMatchesDao.getById("testUserId")).thenReturn(user);
    when(spyObjectMapper.readValue("{\"token\":\"testToken\"}", FCMToken.class))
        .thenThrow(new JsonProcessingException("json processing exception") {});
    // when
    APIGatewayProxyResponseEvent response = handler.handleRequest(input, null);
    // then
    assertNotNull(response);
    assertEquals(400, response.getStatusCode());
  }
}
