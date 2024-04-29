package com.mtjworldcup.gettypersrank;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.gettypersrank.mapper.UserMapper;
import com.mtjworldcup.gettypersrank.model.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Handler
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  public static final Logger log = LoggerFactory.getLogger(Handler.class);

  private final ObjectMapper objectMapper;
  private final MatchesDao matchesDao;

  public Handler() {
    this.objectMapper = new ObjectMapper();
    this.matchesDao = new MatchesDao();
  }

  public Handler(ObjectMapper objectMapper, MatchesDao matchesDao) {
    this.objectMapper = objectMapper;
    this.matchesDao = matchesDao;
  }

  @Override
  public APIGatewayProxyResponseEvent handleRequest(
      APIGatewayProxyRequestEvent input, Context context) {
    try {
      List<Match> users = matchesDao.getUsers();
      List<UserDto> userDtos = UserMapper
              .toUserDto(users)
              .stream()
              .sorted()
              .toList();
      String responseBody = objectMapper.writeValueAsString(userDtos);
      return new APIGatewayProxyResponseEvent().withBody(responseBody).withStatusCode(200);
    } catch (JsonProcessingException e) {
      log.warn("Failed to serialize response. Cause: {}", e.getMessage());
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(500)
          .withBody("Internal Server Error");
    } catch (Exception e) {
      log.warn("Failed to process request. Cause: {}", e.getMessage());
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(500)
          .withBody("Internal Server Error");
    }
  }
}
