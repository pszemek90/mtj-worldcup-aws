package com.mtjworldcup.handlefinishedmatch.service;

import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.handlefinishedmatch.exception.UnsubscribedException;
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
import software.amazon.awssdk.services.sns.model.SubscribeRequest;

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
            subscribeToTopic(endpointArn);
            publishMessage(endpointArn, finishedMatch, poolPerUser);
          } catch (UnsubscribedException e) {
            log.info("User {} is unsubscribed. Hence not sending message.", user.getPrimaryId());
          } catch (Exception e) {
            log.error("Failed to send message to user: {}", user.getPrimaryId(), e);
          }
        });
  }

  private void subscribeToTopic(String endpointArn) {
    try {
      log.info("Subscribing endpoint arn {} to topic", endpointArn);
      SubscribeRequest request =
          SubscribeRequest.builder()
              .protocol("application")
              .endpoint(endpointArn)
              .topicArn(System.getenv("SNS_TOPIC_ARN"))
              .build();
      snsClient.subscribe(request);
      log.info("Endpoint arn {} subscribed to topic", endpointArn);
    } catch (Exception e) {
      log.warn(
          "Subscribing endpoint arn {} to topic failed. Cause: {}", endpointArn, e.getMessage());
    }
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
    log.info("Retrieving endpointArn for user {}", user.getPrimaryId());
    String token = user.getFcmToken();
    log.info("User {} has FCM token: {}", user.getPrimaryId(), token);
    if (token == null) {
      log.warn("User {} does not have FCM token set", user.getPrimaryId());
      throw new UnsubscribedException("User does not have FCM token set");
    }
    log.info("Getting endpoint arn for user {}", user.getPrimaryId());
    String endpointArn = user.getEndpointArn();
    log.info("User {} has endpointArn: {}", user.getPrimaryId(), endpointArn);

    boolean updateNeeded = false;
    boolean createNeeded = (null == endpointArn);

    if (createNeeded) {
      log.info("Create endpoint needed for user {}: {}", user.getPrimaryId(), createNeeded);
      endpointArn = createEndpoint(token, user);
      createNeeded = false;
    }

    try {
      GetEndpointAttributesRequest getAttribuesRequest =
          GetEndpointAttributesRequest.builder().endpointArn(endpointArn).build();
      log.info("Getting endpoint attributes for user {}", user.getPrimaryId());
      GetEndpointAttributesResponse endpointAttributes =
          snsClient.getEndpointAttributes(getAttribuesRequest);
      updateNeeded =
          !endpointAttributes.attributes().get("Token").equals(token)
              || !endpointAttributes.attributes().get("Enabled").equalsIgnoreCase("true");
      log.info("Endpoint update needed for user {}: {}", user.getPrimaryId(), updateNeeded);
    } catch (NotFoundException e) {
      createNeeded = true;
    }

    if (createNeeded) {
      log.info("Second create endpoint needed for user {}: {}", user.getPrimaryId(), createNeeded);
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
      log.info("Update needed. Setting endpoint attributes for user {}", user.getPrimaryId());
      snsClient.setEndpointAttributes(setAttributesRequest);
    }
    return endpointArn;
  }

  private String createEndpoint(String token, Match user) {
    log.info("Creating endpoint for user {} and token {}", user.getPrimaryId(), token);
    String endpointArn;
    try {
      CreatePlatformEndpointRequest platformEndpointRequest =
          CreatePlatformEndpointRequest.builder()
              .token(token)
              .platformApplicationArn(System.getenv("SNS_PLATFORM_APPLICATION_ARN"))
              .customUserData(user.getPrimaryId())
              .build();
      log.info("Creating platform endpoint for user {}", user.getPrimaryId());
      CreatePlatformEndpointResponse platformEndpointResponse =
          snsClient.createPlatformEndpoint(platformEndpointRequest);
      endpointArn = platformEndpointResponse.endpointArn();
    } catch (InvalidParameterException e) {
      log.info("InvalidParameterException with cause: {}", e.getMessage());
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
    log.info("Storing endpointArn {} for user {}", endpointArn, user.getPrimaryId());
    user.setEndpointArn(endpointArn);
    matchesDao.update(user);
  }
}
