package com.mtjworldcup.posttypes;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtjworldcup.common.model.TypingStatus;
import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.cognito.exception.SignatureVerifierException;
import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.posttypes.model.MatchDto;
import com.mtjworldcup.dynamo.model.RecordType;
import com.mtjworldcup.cognito.service.CognitoJwtVerifierService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger log = LoggerFactory.getLogger(Handler.class);

    private final CognitoJwtVerifierService cognitoJwtVerifierService;
    private final MatchesDao matchesDao;
    private final ObjectMapper objectMapper;

    public Handler() {
        this(new CognitoJwtVerifierService(), new MatchesDao(), new ObjectMapper());
    }

    public Handler(CognitoJwtVerifierService cognitoJwtVerifierService, MatchesDao matchesDao, ObjectMapper objectMapper){
        this.cognitoJwtVerifierService = cognitoJwtVerifierService;
        this.matchesDao = matchesDao;
        this.objectMapper = objectMapper;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        String authorizationHeader = input.getHeaders().get("Authorization");
        String bearerToken = authorizationHeader.substring("Bearer ".length());
        try {
            String secondaryId = cognitoJwtVerifierService.checkUser(bearerToken);
            String body = input.getBody();
            log.info("Input body: {}", body);
            MatchDto[] matchDtos = objectMapper.readValue(body, MatchDto[].class);
            List<Match> filteredEntities = Arrays.stream(matchDtos)
                    .map(match -> matchesDao.getById(match.getMatchId()))
                    .filter(entity -> checkMatchDate(entity.getDate(), entity.getStartTime()))
                    .toList();
            List<String> filteredIds = filteredEntities.stream().map(Match::getPrimaryId).toList();
            Map<String, MatchDto> typesToSave = Arrays.stream(matchDtos)
                    .filter(dto -> filteredIds.contains(dto.getMatchId()))
                    .collect(Collectors.toMap(MatchDto::getMatchId, Function.identity()));
            for (Match entity : filteredEntities) {
                MatchDto matchType = typesToSave.get(entity.getPrimaryId());
                entity.setSecondaryId(secondaryId);
                entity.setHomeScore(matchType.getHomeScore());
                entity.setAwayScore(matchType.getAwayScore());
                entity.setRecordType(RecordType.TYPING);
                entity.setTypingStatus(TypingStatus.UNKNOWN);
            }
            if(!filteredEntities.isEmpty()){
                matchesDao.saveTypings(filteredEntities);
            } else {
                return new APIGatewayProxyResponseEvent().withStatusCode(204);
            }
        } catch (SignatureVerifierException e) {
            log.error("Token was not verified! Reason: {}, token: {}", e.getMessage(), bearerToken);
            return new APIGatewayProxyResponseEvent().withStatusCode(403);
        } catch (Exception e) {
            log.error("Unexpected exception occurred while getting subject from token. Exception: {}", e.getMessage());
            return new APIGatewayProxyResponseEvent().withStatusCode(404);
        }
        return new APIGatewayProxyResponseEvent()
                .withHeaders(Map.of("Access-Control-Allow-Origin", "http://localhost:5173"))
                .withStatusCode(201);
    }

    private boolean checkMatchDate(LocalDate matchDate, LocalTime matchTime) {
        if(LocalDate.now().isAfter(matchDate))
            return false;
        if(LocalDate.now().isEqual(matchDate) && LocalTime.now().isAfter(matchTime))
            return false;
        return true;
    }
}
