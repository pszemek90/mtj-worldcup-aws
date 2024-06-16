package com.mtjworldcup.gettypersrank;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.gettypersrank.model.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class HandlerTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final ObjectMapper spyObjectMapper = spy(new ObjectMapper());
  private final MatchesDao mockMatchesDao = mock(MatchesDao.class);

  private Handler handler;

  @BeforeEach
  void setUp() {
    handler = new Handler(spyObjectMapper, mockMatchesDao);
  }

  @Test
  void shouldReturnOneUser_WhenOneUserInDb() throws Exception {
    // given
    when(mockMatchesDao.getUsers()).thenReturn(List.of(new Match()));
    // when
    APIGatewayProxyResponseEvent response = handler.handleRequest(null, null);
    // then
    List<UserDto> users = OBJECT_MAPPER.readValue(response.getBody(), new TypeReference<>() {});
    assertEquals(1, users.size());
  }

  @Test
  void shouldReturnTwoUsers_WhenTwoUsersInDb() throws Exception {
    // given
    Match user1 = new Match();
    user1.setCorrectTypings(10);
    user1.setPool(BigDecimal.ONE);
    Match user2 = new Match();
    user2.setCorrectTypings(20);
    user2.setPool(BigDecimal.TEN);
    when(mockMatchesDao.getUsers()).thenReturn(List.of(user1, user2));
    // when
    APIGatewayProxyResponseEvent response = handler.handleRequest(null, null);
    // then
    List<UserDto> users = OBJECT_MAPPER.readValue(response.getBody(), new TypeReference<>() {});
    assertEquals(2, users.size());
  }

  @Test
  void shouldReturnUsersInDescendingOrderOfCorrectTypings() throws Exception {
    // given
    Match user1 = new Match();
    user1.setCorrectTypings(10);
    Match user2 = new Match();
    user2.setCorrectTypings(20);
    when(mockMatchesDao.getUsers()).thenReturn(List.of(user1, user2));
    // when
    APIGatewayProxyResponseEvent response = handler.handleRequest(null, null);
    // then
    List<UserDto> users = OBJECT_MAPPER.readValue(response.getBody(), new TypeReference<>() {});
    assertEquals(20, users.get(0).correctTypings());
    assertEquals(10, users.get(1).correctTypings());
  }

  @Test
  void shouldReturnInternalServerError_WhenObjectMapperFails() throws Exception {
    // given
    when(mockMatchesDao.getUsers()).thenReturn(List.of(new Match()));
    when(spyObjectMapper.writeValueAsString(List.of(new UserDto(null, 0, null))))
        .thenThrow(new JsonProcessingException("Object Mapper failed") {});
    // when
    APIGatewayProxyResponseEvent response = handler.handleRequest(null, null);
    // then
    assertEquals(500, response.getStatusCode());
    assertEquals("Internal Server Error", response.getBody());
  }

  @Test
  void shouldReturnInternalServerError_WhenMatchesDaoFails() {
    // given
    when(mockMatchesDao.getUsers()).thenThrow(new RuntimeException());
    // when
    APIGatewayProxyResponseEvent response = handler.handleRequest(null, null);
    // then
    assertEquals(500, response.getStatusCode());
    assertEquals("Internal Server Error", response.getBody());
  }
}
