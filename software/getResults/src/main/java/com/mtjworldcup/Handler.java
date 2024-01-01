package com.mtjworldcup;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtjworldcup.dao.MatchesDao;
import com.mtjworldcup.mapper.MatchMapper;
import com.mtjworldcup.model.Match;
import com.mtjworldcup.model.Matches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger log = LoggerFactory.getLogger(Handler.class);

    private final MatchesDao matchesDao;

    public Handler() {
        this(new MatchesDao());
    }

    public Handler(MatchesDao matchesDao) {
        this.matchesDao = matchesDao;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        List<Match> finishedMatches = matchesDao.getFinishedMatches();
        try{
            ObjectMapper objectMapper = new ObjectMapper();
            Matches matches = new Matches(MatchMapper.mapToDto(finishedMatches));
            String body = objectMapper.writeValueAsString(matches);
            return new APIGatewayProxyResponseEvent().withBody(body)
                    .withHeaders(Map.of("Access-Control-Allow-Origin", "http://localhost:5173"))
                    .withStatusCode(200);
        } catch (JsonProcessingException e) {
            log.error("Error occurred while creating a body string. Exception: {}", e.getMessage());
            return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody("Error while creating json body.");
        }
    }
}
