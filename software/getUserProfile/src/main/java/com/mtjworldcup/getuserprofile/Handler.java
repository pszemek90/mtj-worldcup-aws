package com.mtjworldcup.getuserprofile;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.mtjworldcup.cognito.exception.SignatureVerifierException;
import com.mtjworldcup.cognito.service.CognitoJwtVerifierService;
import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.dynamo.model.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>{

    public static final Logger log = LoggerFactory.getLogger(Handler.class);

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
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        String authorizationHeader = input.getHeaders().get("Authorization");
        String bearerToken = authorizationHeader.substring("Bearer ".length());
        try {
            String userId = cognitoJwtVerifierService.checkUser(bearerToken);
            Match user = matchesDao.getById(userId);
            Integer userBalance = Optional.ofNullable(user)
                    .map(Match::getPool)
                    .orElseThrow(() -> new NoSuchElementException("User pool not found for user id: " + userId));
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(Map.of("Access-Control-Allow-Origin", "http://localhost:5173"))
                    .withBody(userBalance.toString());
        } catch (SignatureVerifierException e) {
            log.error("Error while verifying user token: {}", e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(401)
                    .withBody("Unauthorized");
        } catch (NoSuchElementException e) {
            log.error("User not found: {}", e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(404)
                    .withBody("User not found");
        }
    }
}
