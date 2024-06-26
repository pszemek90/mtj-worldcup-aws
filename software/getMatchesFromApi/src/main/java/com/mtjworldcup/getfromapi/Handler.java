package com.mtjworldcup.getfromapi;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtjworldcup.common.model.MatchDto;
import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.getfromapi.mapper.MatchMapper;
import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.getfromapi.service.MatchApiService;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger log = LoggerFactory.getLogger(Handler.class);

    private final MatchesDao matchesDao;

    public Handler() {
        this.matchesDao = new MatchesDao();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        log.info("Fetching matches from api.");
        MatchApiService matchService = new MatchApiService(new OkHttpClient(), new ObjectMapper());
        List<MatchDto> matchesFromApi = matchService.getMatchesFromApi();
        log.info("Matches from api: {}", matchesFromApi);
        List<MatchDto> translateCountries = MatchMapper.translateCountries(matchesFromApi);
        log.info("Matches after translation: {}", translateCountries);
        List<Match> entitiesToPersist = MatchMapper.mapToEntity(translateCountries);
        log.info("Entities for persist: {}", entitiesToPersist);
        matchesDao.saveIfNotExists(entitiesToPersist);
        return new APIGatewayProxyResponseEvent().withStatusCode(200);
    }
}
