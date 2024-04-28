package com.mtjworldcup.handlefinishedmatch;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import com.mtjworldcup.dynamo.dao.MatchesDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class Handler implements RequestHandler<DynamodbEvent, Void> {

  private static final Logger log = LoggerFactory.getLogger(Handler.class);

  private final MatchesDao matchesDao;

  public Handler() {
    this.matchesDao = new MatchesDao();
  }

  public Handler(MatchesDao matchesDao) {
    this.matchesDao = matchesDao;
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
                        log.info("Updating DB entries for match: {}", primaryId);
                        matchesDao.handleFinishedMatch(primaryId);
                      });
            });
    return null;
  }
}
