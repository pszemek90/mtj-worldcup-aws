package com.mtjworldcup.handlefinishedmatch;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import com.mtjworldcup.handlefinishedmatch.service.FinishedMatchService;

import java.math.BigDecimal;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Handler implements RequestHandler<DynamodbEvent, Void> {

  private static final Logger log = LoggerFactory.getLogger(Handler.class);

  private final FinishedMatchService finishedMatchService;

  public Handler() {
    this.finishedMatchService = new FinishedMatchService();
  }

  public Handler(FinishedMatchService finishedMatchService) {
      this.finishedMatchService = finishedMatchService;
  }

  @Override
  public Void handleRequest(DynamodbEvent input, Context context) {
    log.info("Received event: {}", input);
    input
        .getRecords()
        .forEach(
            streamRecord -> {
              log.info("DynamoDB Record: {}", streamRecord.getDynamodb());
              if (!isAlreadyProcessed(streamRecord)) {
                  Optional.of(streamRecord)
                          .map(DynamodbEvent.DynamodbStreamRecord::getDynamodb)
                          .map(StreamRecord::getKeys)
                          .map(keys -> keys.get("primary_id"))
                          .map(AttributeValue::getS)
                          .ifPresent(
                                  primaryId -> {
                                      log.info("Handling finished match: {}", primaryId);
                                      finishedMatchService.handleFinishedMatch(primaryId);
                                  });
              } else {
                  log.info("Record was already processed. Record: {}", streamRecord);
              }
            });
    return null;
  }

    private boolean isAlreadyProcessed(DynamodbEvent.DynamodbStreamRecord streamRecord) {
      try {
          return Optional.of(streamRecord)
                  .map(DynamodbEvent.DynamodbStreamRecord::getDynamodb)
                  .map(StreamRecord::getNewImage)
                  .map(newImage -> newImage.get("pool"))
                  .map(AttributeValue::getN)
                  .stream()
                  .anyMatch(pool -> new BigDecimal(pool).compareTo(BigDecimal.ZERO) == 0);
      } catch (Exception e) {
          log.warn("Exception while checking if the dynamodb event was already processed! Setting already processed to true. Cause: {}",
                  e.getMessage());
          return true;
      }
    }
}
