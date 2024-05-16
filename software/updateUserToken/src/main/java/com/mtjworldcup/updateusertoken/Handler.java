package com.mtjworldcup.updateusertoken;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtjworldcup.cognito.exception.SignatureVerifierException;
import com.mtjworldcup.cognito.service.CognitoJwtVerifierService;
import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.sns.SnsService;
import com.mtjworldcup.updateusertoken.model.FCMToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger log = LoggerFactory.getLogger(Handler.class);

    private final CognitoJwtVerifierService cognitoJwtVerifierService;
    private final MatchesDao matchesDao;
    private final ObjectMapper objectMapper;
    private final SnsService snsService;

    public Handler(CognitoJwtVerifierService cognitoJwtVerifierService, MatchesDao matchesDao, ObjectMapper objectMapper, SnsService snsService) {
        this.cognitoJwtVerifierService = cognitoJwtVerifierService;
        this.matchesDao = matchesDao;
        this.objectMapper = objectMapper;
        this.snsService = snsService;
    }

    public Handler() {
        this.cognitoJwtVerifierService = new CognitoJwtVerifierService();
        this.matchesDao = new MatchesDao();
        this.objectMapper = new ObjectMapper();
        this.snsService = new SnsService();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            String authorizationHeader = input.getHeaders().get("Authorization");
            String bearerToken = authorizationHeader.substring("Bearer ".length());
            String userId = cognitoJwtVerifierService.checkUser(bearerToken);
            log.info("Updating user token for user: {}", userId);
            Match user = matchesDao.getById(userId);
            String requestBody = Optional.of(input)
                    .map(APIGatewayProxyRequestEvent::getBody)
                    .orElseThrow(() -> new IllegalArgumentException("Request body cannot be empty!"));
            FCMToken fcmToken = objectMapper.readValue(requestBody, FCMToken.class);
            String token = fcmToken.token();
            user.setFcmToken(token);
            if(user.getEndpointArn() == null) {
                String endpointArn = snsService.retrieveEndpointArn(token, null, userId);
                user.setEndpointArn(endpointArn);
                String subscriptionArn = snsService.subscribeToTopic(endpointArn);
                user.setSubscriptionArn(subscriptionArn);
            } else {
                snsService.updatePlatformEndpoint(user.getEndpointArn(), token);
            }
            matchesDao.update(user);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(204);
        } catch (SignatureVerifierException e) {
            log.warn("Signature verification failed. Cause: {}", e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(401);
        } catch (JsonMappingException e) {
            log.warn("Could not map request body to FCMToken. Cause: {}", e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400);
        } catch (JsonProcessingException e) {
            log.warn("Could not process request body. Cause: {}", e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400);
        }
    }
}
