package com.mtjworldcup;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.mtjworldcup.mapper.MatchMapper;
import com.mtjworldcup.model.Match;
import com.mtjworldcup.model.MatchDto;
import com.mtjworldcup.service.MatchApiService;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteResult;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;

public class MatchHandler implements RequestHandler<Object, String> {

    private static final String BASE_API_URL = "https://api-football-v1.p.rapidapi.com/v3/fixtures?league=39&season=2023"; //todo think what to do with a season, maybe get rid of it?
    private static final Logger log = LoggerFactory.getLogger(MatchHandler.class);


    private final DynamoDbClient ddb;
    private final DynamoDbEnhancedClient enhancedClient;

    public MatchHandler() {
        ddb = DynamoDbClient.builder()
                .region(Region.EU_CENTRAL_1)
                .build();
        enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(ddb)
                .build();
    }

    @Override
    public String handleRequest(Object input, Context context) {
        log.info("Fetching matches from api.");
        String matchesTableName = System.getenv("MATCHES_TABLE_NAME");
        DynamoDbTable<Match> matches = enhancedClient.table(matchesTableName, TableSchema.fromBean(Match.class));
        MatchApiService matchService = new MatchApiService(new OkHttpClient());
        List<MatchDto> matchesFromApi = matchService.getMatchesFromApi(BASE_API_URL);
        log.info("Matches from api: {}", matchesFromApi);
        List<Match> entitiesToPersist = MatchMapper.mapToEntity(matchesFromApi);
        log.info("Entities for persist: {}", entitiesToPersist);
        List<WriteBatch> writeBatches = entitiesToPersist.stream()
                .map(entity -> WriteBatch.builder(Match.class)
                        .mappedTableResource(matches)
                        .addPutItem(entity)
                        .build())
                .toList();
        BatchWriteItemEnhancedRequest batchRequest = BatchWriteItemEnhancedRequest.builder()
                .writeBatches(writeBatches)
                .build();
        BatchWriteResult batchWriteResult = enhancedClient.batchWriteItem(batchRequest);
        List<Match> unprocessedEntities = batchWriteResult.unprocessedPutItemsForTable(matches);
        if(!unprocessedEntities.isEmpty()) {
            log.warn("Unprocessed entities: {}", unprocessedEntities);
            return "Matches batch written with errors";
        } else {
            return "Matches batch written successfully";
        }
    }
}
