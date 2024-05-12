package com.mtjworldcup.deleteregistrationtoken;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.mtjworldcup.cognito.exception.SignatureVerifierException;
import com.mtjworldcup.cognito.service.CognitoJwtVerifierService;
import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.dynamo.model.Match;

import java.util.Map;

public class Handler
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final CognitoJwtVerifierService cognitoJwtVerifierService;
  private final MatchesDao matchesDao;

  public Handler() {
    this.cognitoJwtVerifierService = new CognitoJwtVerifierService();
    this.matchesDao = new MatchesDao();
  }

  public Handler(CognitoJwtVerifierService cognitoJwtVerifierService, MatchesDao matchesDao) {
    this.cognitoJwtVerifierService = cognitoJwtVerifierService;
    this.matchesDao = matchesDao;
  }

  @Override
  public APIGatewayProxyResponseEvent handleRequest(
      APIGatewayProxyRequestEvent input, Context context) {
    try {
      String authorizationHeader = input.getHeaders().get("Authorization");
      String bearerToken = authorizationHeader.substring("Bearer ".length());
      String userId = cognitoJwtVerifierService.checkUser(bearerToken);
      Match user = matchesDao.getById(userId);
      user.setFcmToken(null);
      matchesDao.update(user);
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(200);
    } catch (SignatureVerifierException e) {
      return new APIGatewayProxyResponseEvent().withStatusCode(401);
    } catch (Exception e) {
      return new APIGatewayProxyResponseEvent().withStatusCode(500);
    }
  }
}
