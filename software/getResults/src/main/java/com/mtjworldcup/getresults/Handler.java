package com.mtjworldcup.getresults;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.getresults.mapper.MatchMapper;
import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.getresults.model.MatchDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        Map<LocalDate, List<MatchDto>> finishedMatchesGroupedByDate = matchesDao.getFinishedMatches()
                .stream()
                .collect(Collectors.groupingBy(Match::getDate, Collectors.mapping(MatchMapper::mapToDto, Collectors.toList())));
        log.debug("Finished matches returned: {}", finishedMatchesGroupedByDate);
        try{
            ObjectMapper objectMapper = new ObjectMapper();
            String body = objectMapper.writeValueAsString(finishedMatchesGroupedByDate);
            return new APIGatewayProxyResponseEvent().withBody(body)
                    .withStatusCode(200);
        } catch (JsonProcessingException e) {
            log.error("Error occurred while creating a body string. Exception: {}", e.getMessage());
            return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody("Error while creating json body.");
        }
    }
}
