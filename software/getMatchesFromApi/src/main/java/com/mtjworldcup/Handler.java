package com.mtjworldcup;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.mapper.MatchMapper;
import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.model.MatchDto;
import com.mtjworldcup.service.MatchApiService;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteResult;

import java.util.List;

public class Handler implements RequestHandler<Object, String> {

    private static final String BASE_API_URL = "https://api-football-v1.p.rapidapi.com/v3";
    private static final Logger log = LoggerFactory.getLogger(Handler.class);

    private final MatchesDao matchesDao;

    public Handler() {
        this.matchesDao = new MatchesDao();
    }

    @Override
    public String handleRequest(Object input, Context context) {
        log.info("Fetching matches from api.");
        MatchApiService matchService = new MatchApiService(new OkHttpClient(), new ObjectMapper());
        List<MatchDto> matchesFromApi = matchService.getMatchesFromApi(BASE_API_URL);
        log.info("Matches from api: {}", matchesFromApi);
        List<Match> entitiesToPersist = MatchMapper.mapToEntity(matchesFromApi);
        log.info("Entities for persist: {}", entitiesToPersist);
        BatchWriteResult batchWriteResult = matchesDao.save(entitiesToPersist);
        List<Match> unprocessedEntities = batchWriteResult.unprocessedPutItemsForTable(matchesDao.getMatchTable());
        if(!unprocessedEntities.isEmpty()) {
            log.warn("Unprocessed entities: {}", unprocessedEntities);
            return "Matches batch written with errors";
        } else {
            return "Matches batch written successfully";
        }
    }
}
