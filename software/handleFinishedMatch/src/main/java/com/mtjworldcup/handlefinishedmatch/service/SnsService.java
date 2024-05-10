package com.mtjworldcup.handlefinishedmatch.service;

import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.dynamo.model.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointRequest;
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointResponse;
import software.amazon.awssdk.services.sns.model.GetEndpointAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetEndpointAttributesResponse;
import software.amazon.awssdk.services.sns.model.InvalidParameterException;
import software.amazon.awssdk.services.sns.model.NotFoundException;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SetEndpointAttributesRequest;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SnsService {

  private static final Logger log = LoggerFactory.getLogger(SnsService.class);

  private final SnsClient snsClient;
  private final MatchesDao matchesDao;

  public SnsService() {
    this.snsClient = SnsClient.create();
    this.matchesDao = new MatchesDao();
  }

  public SnsService(SnsClient snsClient, MatchesDao matchesDao) {
    this.snsClient = snsClient;
    this.matchesDao = matchesDao;
  }

  public void sendMessages(List<Match> users, Match finishedMatch, BigDecimal poolPerUser) {
    users.forEach(
        user -> {
          try {
            String endpointArn = retrieveEndpointArn(user);
            publishMessage(endpointArn, finishedMatch, poolPerUser);
          } catch (Exception e) {
            log.error("Failed to send message to user: {}", user.getPrimaryId(), e);
          }
        });
  }

  private void publishMessage(String endpointArn, Match finishedMatch, BigDecimal poolPerUser) {
    if (endpointArn == null) {
      log.warn("EndpointArn is null, skipping message sending");
      return;
    }
    PublishRequest request =
        PublishRequest.builder()
            .message(
                String.format(
                    """
                    {
                      "GCM": "{ \\"notification\\": { \\"title\\": \\"Wygrana!\\", \\"body\\": \\"Wygrana %.2fzł w meczu %s - %s\\" } }"
                    }
                    """,
                    poolPerUser, finishedMatch.getHomeTeam(), finishedMatch.getAwayTeam()))
            .messageStructure("json")
            .targetArn(endpointArn)
            .build();
    log.info("Sending message to endpoint: {}. Payload: {}", endpointArn, request.message());
    snsClient.publish(request);
  }

  private String retrieveEndpointArn(Match user) {
    String token = user.getFcmToken();
    if (token == null) {
      log.warn("User {} does not have FCM token set", user.getPrimaryId());
      return null;
    }
    String endpointArn = user.getEndpointArn();

    boolean updateNeeded = false;
    boolean createNeeded = (null == endpointArn);

    if (createNeeded) {
      endpointArn = createEndpoint(token, user);
      createNeeded = false;
    }

    try {
      GetEndpointAttributesRequest getAttribuesRequest =
          GetEndpointAttributesRequest.builder().endpointArn(endpointArn).build();
      GetEndpointAttributesResponse endpointAttributes =
          snsClient.getEndpointAttributes(getAttribuesRequest);
      updateNeeded =
          !endpointAttributes.attributes().get("Token").equals(token)
              || !endpointAttributes.attributes().get("Enabled").equalsIgnoreCase("true");
    } catch (NotFoundException e) {
      createNeeded = true;
    }

    if (createNeeded) {
      endpointArn = createEndpoint(token, user);
    }

    if (updateNeeded) {
      Map<String, String> attributes = new HashMap<>();
      attributes.put("Token", token);
      attributes.put("Enabled", "true");
      SetEndpointAttributesRequest setAttributesRequest =
          SetEndpointAttributesRequest.builder()
              .endpointArn(endpointArn)
              .attributes(attributes)
              .build();
      snsClient.setEndpointAttributes(setAttributesRequest);
    }
    return endpointArn;
  }

  private String createEndpoint(String token, Match user) {
    String endpointArn;
    try {
      CreatePlatformEndpointRequest platformEndpointRequest =
          CreatePlatformEndpointRequest.builder()
              .token(token)
              .platformApplicationArn(System.getenv("SNS_PLATFORM_APPLICATION_ARN"))
              .build();
      CreatePlatformEndpointResponse platformEndpointResponse =
          snsClient.createPlatformEndpoint(platformEndpointRequest);
      endpointArn = platformEndpointResponse.endpointArn();
    } catch (InvalidParameterException e) {
      String message = e.getMessage();
      Pattern pattern =
          Pattern.compile(".*Endpoint (arn:aws:sns[^ ]+) already exists with the same Token.*");
      Matcher matcher = pattern.matcher(message);
      if (matcher.matches()) {
        endpointArn = matcher.group(1);
      } else {
        log.warn("Failed to create endpoint. Returning null. Cause: {}", e.getMessage());
        return null;
      }
    }
    storeEndpointArn(endpointArn, user);
    return endpointArn;
  }

  private void storeEndpointArn(String endpointArn, Match user) {
    user.setEndpointArn(endpointArn);
    matchesDao.update(user);
  }
}
