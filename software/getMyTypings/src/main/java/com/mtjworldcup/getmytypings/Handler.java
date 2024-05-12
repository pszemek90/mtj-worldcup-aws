package com.mtjworldcup.getmytypings;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.cognito.exception.SignatureVerifierException;
import com.mtjworldcup.getmytypings.mapper.TypingMapper;
import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.cognito.service.CognitoJwtVerifierService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger log = LoggerFactory.getLogger(Handler.class);

    private final ObjectMapper objectMapper;
    private final CognitoJwtVerifierService cognitoJwtVerifierService;
    private final MatchesDao matchesDao;

    public Handler(ObjectMapper objectMapper, CognitoJwtVerifierService cognitoJwtVerifierService, MatchesDao matchesDao) {
        this.objectMapper = objectMapper;
        this.cognitoJwtVerifierService = cognitoJwtVerifierService;
        this.matchesDao = matchesDao;
    }

    public Handler() {
        this.objectMapper = new ObjectMapper();
        this.cognitoJwtVerifierService = new CognitoJwtVerifierService();
        this.matchesDao = new MatchesDao();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            String authorizationHeader = input.getHeaders().get("Authorization");
            String bearerToken = authorizationHeader.substring("Bearer ".length());
            String userId = cognitoJwtVerifierService.checkUser(bearerToken);
            log.info("Getting typings for user: {}", userId);
            List<Match> typingRecords = matchesDao.getTypingsByUserId(userId);
            var typings = TypingMapper.mapToDto(typingRecords);
            log.info("Typings found: {}, userId: {}", typings, userId);
            String body = objectMapper.writeValueAsString(typings);
            return new APIGatewayProxyResponseEvent()
                    .withBody(body)
                    .withStatusCode(200);
        } catch (JsonProcessingException e) {
            log.error("Exception while writing typings to JSON. Cause: {}", e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withBody("Could not get your typings. Please try again later.")
                    .withStatusCode(500);
        } catch (SignatureVerifierException e) {
            log.error("Exception while verifying user. Cause: {}", e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withBody("Could not get your typings. Unauthorized access!")
                    .withStatusCode(401);
        }
    }
}
