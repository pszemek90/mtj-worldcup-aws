package com.mtjworldcup.getuserhistory;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtjworldcup.cognito.exception.SignatureVerifierException;
import com.mtjworldcup.cognito.service.CognitoJwtVerifierService;
import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.dynamo.model.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Handler
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  public static final Logger log = LoggerFactory.getLogger(Handler.class);

  private final ObjectMapper objectMapper;
  private final MatchesDao matchesDao;
  private final CognitoJwtVerifierService cognitoJwtVerifierService;

  public Handler() {
    this.objectMapper = new ObjectMapper();
    this.matchesDao = new MatchesDao();
    this.cognitoJwtVerifierService = new CognitoJwtVerifierService();
  }

  public Handler(ObjectMapper objectMapper, MatchesDao matchesDao, CognitoJwtVerifierService cognitoJwtVerifierService) {
    this.objectMapper = objectMapper;
    this.matchesDao = matchesDao;
    this.cognitoJwtVerifierService = cognitoJwtVerifierService;
  }

  @Override
  public APIGatewayProxyResponseEvent handleRequest(
      APIGatewayProxyRequestEvent input, Context context) {
    try {
      String authorizationHeader = input.getHeaders().get("Authorization");
      if (authorizationHeader == null) {
        return new APIGatewayProxyResponseEvent().withBody("Unauthorized").withStatusCode(401);
      }
      String userId =
          cognitoJwtVerifierService.checkUser(authorizationHeader.substring("Bearer ".length()));
      List<Match> userMessages = matchesDao.getMessagesByUserId(userId);
      String responseBody = objectMapper.writeValueAsString(userMessages);
      return new APIGatewayProxyResponseEvent().withBody(responseBody).withStatusCode(200);
    } catch (JsonProcessingException e) {
      log.warn("Failed to serialize message. Cause: {}", e.getMessage());
      return new APIGatewayProxyResponseEvent()
          .withBody("Server error occurred!")
          .withStatusCode(500);
    } catch (SignatureVerifierException e) {
      log.warn("Failed to verify signature. Cause: {}", e.getMessage());
      return new APIGatewayProxyResponseEvent().withBody("Unauthorized").withStatusCode(401);
    } catch (Exception e) {
      log.error("Failed to process request. Cause: {}", e.getMessage());
      return new APIGatewayProxyResponseEvent().withBody("Server error occurred!").withStatusCode(500);
    }
  }
}
