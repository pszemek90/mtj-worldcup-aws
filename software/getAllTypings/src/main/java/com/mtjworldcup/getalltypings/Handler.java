package com.mtjworldcup.getalltypings;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mtjworldcup.getalltypings.service.TypingsService;
import org.slf4j.Logger;

public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    public static final Logger log = org.slf4j.LoggerFactory.getLogger(Handler.class);

    private final ObjectMapper objectMapper;
    private final TypingsService typingsService;

    public Handler() {
        this(new ObjectMapper().registerModule(new JavaTimeModule()), new TypingsService());
    }
    public Handler(ObjectMapper objectMapper, TypingsService typingsService) {
        this.objectMapper = objectMapper;
        this.typingsService = typingsService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            var allTypings = typingsService.getAllTypings();
            String stringBody = objectMapper.writeValueAsString(allTypings);
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody(stringBody);
        } catch (JsonProcessingException e) {
            log.error("Error occurred while creating a body string. Exception: {}", e.getMessage());
            return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody("Server error occurred. Please contact support.");
        } catch (Exception e) {
            log.error("Unexpected server error occurred. Exception: {}", e.getMessage());
            return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody("Server error occurred. Please contact support.");
        }
    }
}
