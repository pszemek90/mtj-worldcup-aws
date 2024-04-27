package com.mtjworldcup.handlefinishedmatch;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Handler implements RequestHandler<DynamodbEvent, Void> {

    private static final Logger log = LoggerFactory.getLogger(Handler.class);

    @Override
    public Void handleRequest(DynamodbEvent input, Context context) {
        log.info("Received event: {}", input);
        input.getRecords().forEach(streamRecord -> {
            log.info("Event ID: {}", streamRecord.getEventID());
            log.info("Event Name: {}", streamRecord.getEventName());
            log.info("Event Source: {}", streamRecord.getEventSource());
            log.info("DynamoDB Record: {}", streamRecord.getDynamodb().getNewImage());
        });
        return null;
    }
}
