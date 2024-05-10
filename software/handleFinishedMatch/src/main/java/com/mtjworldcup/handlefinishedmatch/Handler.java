package com.mtjworldcup.handlefinishedmatch;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import com.mtjworldcup.handlefinishedmatch.service.FinishedMatchService;
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
            });
    return null;
  }
}
