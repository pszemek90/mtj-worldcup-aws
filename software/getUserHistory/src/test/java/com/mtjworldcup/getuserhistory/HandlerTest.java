package com.mtjworldcup.getuserhistory;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtjworldcup.cognito.exception.SignatureVerifierException;
import com.mtjworldcup.cognito.service.CognitoJwtVerifierService;
import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.dynamo.model.Match;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class HandlerTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final ObjectMapper spyObjectMapper = spy(ObjectMapper.class);
  private final MatchesDao mockMatchesDao = mock(MatchesDao.class);
  private final CognitoJwtVerifierService mockCognitoJwtVerifierService =
      mock(CognitoJwtVerifierService.class);

  private Handler handler;

  @BeforeEach
  void setUp() {
    handler = new Handler(spyObjectMapper, mockMatchesDao, mockCognitoJwtVerifierService);
  }

  @Test
  void shouldReturnOneMessage_WhenOneMessageInDb() throws Exception {
    // given
    when(mockCognitoJwtVerifierService.checkUser("token")).thenReturn("testUserId");
    when(mockMatchesDao.getMessagesByUserId("testUserId")).thenReturn(List.of(new Match()));
    APIGatewayProxyRequestEvent request =
        new APIGatewayProxyRequestEvent().withHeaders(Map.of("Authorization", "Bearer token"));
    // when
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);
    // then
    String responseBody = response.getBody();
    List<Match> messages = OBJECT_MAPPER.readValue(responseBody, new TypeReference<>() {});
    assertEquals(1, messages.size());
  }

  @Test
  void shouldReturnTwoMessages_WhenTwoMessagesForUserInDb() throws Exception {
    // given
    when(mockCognitoJwtVerifierService.checkUser("token")).thenReturn("testUserId");
    when(mockMatchesDao.getMessagesByUserId("testUserId"))
        .thenReturn(List.of(new Match(), new Match()));
    APIGatewayProxyRequestEvent request =
        new APIGatewayProxyRequestEvent().withHeaders(Map.of("Authorization", "Bearer token"));
    // when
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);
    // then
    String responseBody = response.getBody();
    List<Match> messages = OBJECT_MAPPER.readValue(responseBody, new TypeReference<>() {});
    assertEquals(2, messages.size());
  }

  @Test
  void shouldReturnUnauthorized_WhenNoAuthorizationHeader() {
    // given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withHeaders(Map.of());
    // when
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);
    // then
    assertEquals(401, response.getStatusCode());
    assertEquals("Unauthorized", response.getBody());
  }

  @Test
  void shouldReturnUnauthorized_WhenCognitoThrowsException() throws Exception {
    // given
    when(mockCognitoJwtVerifierService.checkUser("token"))
        .thenThrow(new SignatureVerifierException("Something went wrong!"));
    APIGatewayProxyRequestEvent request =
        new APIGatewayProxyRequestEvent().withHeaders(Map.of("Authorization", "Bearer token"));
    // when
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);
    // then
    assertEquals(401, response.getStatusCode());
    assertEquals("Unauthorized", response.getBody());
  }

  @Test
  void shouldReturnServerError_WhenJsonProcessingException() throws Exception {
    // given
    when(mockCognitoJwtVerifierService.checkUser("token")).thenReturn("testUserId");
    List<Match> userMessages = List.of(new Match());
    when(mockMatchesDao.getMessagesByUserId("testUserId")).thenReturn(userMessages);
    when(spyObjectMapper.writeValueAsString(userMessages))
        .thenThrow(new JsonProcessingException("Something went wrong!") {});
    APIGatewayProxyRequestEvent request =
        new APIGatewayProxyRequestEvent().withHeaders(Map.of("Authorization", "Bearer token"));
    // when
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);
    // then
    assertEquals(500, response.getStatusCode());
    assertEquals("Server error occurred!", response.getBody());
  }

  @Test
  void shouldReturnServerError_WhenNoHeadersInRequest() {
    // given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    // when
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);
    // then
    assertEquals(500, response.getStatusCode());
    assertEquals("Server error occurred!", response.getBody());
  }
}
