package com.mtjworldcup.sns;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mtjworldcup.sns.exception.UnsubscribedException;
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
import software.amazon.awssdk.services.sns.model.SubscribeResponse;

public class SnsService {

  private static final Logger log = LoggerFactory.getLogger(SnsService.class);
  public static final String TOKEN = "Token";

  private final SnsClient snsClient;

  public SnsService() {
    this.snsClient = SnsClient.create();
  }

  public SnsService(SnsClient snsClient) {
    this.snsClient = snsClient;
  }

  public String subscribeToTopic(String endpointArn) {
    try {
      log.info("Subscribing endpoint arn {} to topic", endpointArn);
      SubscribeRequest request =
          SubscribeRequest.builder()
              .protocol("application")
              .endpoint(endpointArn)
              .topicArn(System.getenv("SNS_TOPIC_ARN"))
              .build();
      SubscribeResponse subscriptionResponse = snsClient.subscribe(request);
      String subsriptionArn = subscriptionResponse.subscriptionArn();
      log.info("Endpoint arn {} subscribed to topic. Subscription arn: {}", endpointArn, subsriptionArn);
      return subsriptionArn;
    } catch (Exception e) {
      log.warn(
          "Subscribing endpoint arn {} to topic failed. Cause: {}", endpointArn, e.getMessage());
    }
    return null;
  }

  public void publishMessage(String endpointArn, String message) {
    if (endpointArn == null) {
      log.warn("EndpointArn is null, skipping message sending");
      return;
    }
    PublishRequest request =
        PublishRequest.builder()
            .message(message)
            .messageStructure("json")
            .targetArn(endpointArn)
            .build();
    log.info("Sending message to endpoint: {}. Payload: {}", endpointArn, request.message());
    snsClient.publish(request);
  }

  public String retrieveEndpointArn(String token, String endpointArn, String username) {
    log.info("Retrieving endpointArn for user {}", username);
    log.info("User {} has FCM token: {}", username, token);
    if (token == null) {
      log.warn("User {} does not have FCM token set", username);
      throw new UnsubscribedException("User does not have FCM token set");
    }
    log.info("Getting endpoint arn for user {}", username);
    log.info("User {} has endpointArn: {}", username, endpointArn);

    boolean updateNeeded = false;
    boolean createNeeded = (null == endpointArn);

    if (createNeeded) {
      log.info("Create endpoint needed for user {}: {}", username, createNeeded);
      endpointArn = createEndpoint(token, username);
      createNeeded = false;
    }

    try {
      GetEndpointAttributesRequest getAttribuesRequest =
          GetEndpointAttributesRequest.builder().endpointArn(endpointArn).build();
      log.info("Getting endpoint attributes for user {}", username);
      GetEndpointAttributesResponse endpointAttributes =
          snsClient.getEndpointAttributes(getAttribuesRequest);
      updateNeeded =
          !endpointAttributes.attributes().get(TOKEN).equals(token)
              || !endpointAttributes.attributes().get("Enabled").equalsIgnoreCase("true");
      log.info("Endpoint update needed for user {}: {}", username, updateNeeded);
    } catch (NotFoundException e) {
      createNeeded = true;
    }

    if (createNeeded) {
      log.info("Second create endpoint needed for user {}: {}", username, createNeeded);
      endpointArn = createEndpoint(token, username);
    }

    if (updateNeeded) {
      Map<String, String> attributes = new HashMap<>();
      attributes.put(TOKEN, token);
      attributes.put("Enabled", "true");
      SetEndpointAttributesRequest setAttributesRequest =
          SetEndpointAttributesRequest.builder()
              .endpointArn(endpointArn)
              .attributes(attributes)
              .build();
      log.info("Update needed. Setting endpoint attributes for user {}", username);
      snsClient.setEndpointAttributes(setAttributesRequest);
    }
    return endpointArn;
  }

  private String createEndpoint(String token, String username) {
    log.info("Creating endpoint for user {} and token {}", username, token);
    String endpointArn;
    try {
      CreatePlatformEndpointRequest platformEndpointRequest =
          CreatePlatformEndpointRequest.builder()
              .token(token)
              .platformApplicationArn(System.getenv("SNS_PLATFORM_APPLICATION_ARN"))
              .customUserData(username)
              .build();
      log.info("Creating platform endpoint for user {}", username);
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
    return endpointArn;
  }

  public void updatePlatformEndpoint(String endpointArn, String token) {
    Map<String, String> attributes = new HashMap<>();
    attributes.put(TOKEN, token);
    SetEndpointAttributesRequest setAttributesRequest =
        SetEndpointAttributesRequest.builder()
            .endpointArn(endpointArn)
            .attributes(attributes)
            .build();
    log.info("Setting new token for endpointArn {}", endpointArn);
    snsClient.setEndpointAttributes(setAttributesRequest);
  }

  public void unsubscribeFromTopic(String subscriptionArn) {
    snsClient.unsubscribe(builder -> builder.subscriptionArn(subscriptionArn));
  }

  public void deleteEndpoint(String endpointArn) {
    snsClient.deleteEndpoint(builder -> builder.endpointArn(endpointArn));
  }
}
