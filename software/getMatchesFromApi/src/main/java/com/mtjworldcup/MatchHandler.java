package com.mtjworldcup;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.mtjworldcup.mapper.MatchMapper;
import com.mtjworldcup.model.Match;
import com.mtjworldcup.model.MatchDto;
import com.mtjworldcup.service.MatchApiService;
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
    private DynamoDbClient ddb;
    private DynamoDbEnhancedClient enhancedClient;
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
        LambdaLogger logger = context.getLogger();
        String matchesTableName = System.getenv("MATCHES_TABLE_NAME");
        DynamoDbTable<Match> matches = enhancedClient.table(matchesTableName, TableSchema.fromBean(Match.class));
        MatchApiService matchService = new MatchApiService();
        List<MatchDto> matchesFromApi = matchService.getMatchesFromApi(logger);
        logger.log("Matches from api: " + matchesFromApi, LogLevel.INFO);
        List<Match> entitiesToPersist = MatchMapper.mapToEntity(matchesFromApi);
        logger.log("Entities for persist: " + entitiesToPersist, LogLevel.INFO);
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
            logger.log("Unprocessed entities: " + unprocessedEntities, LogLevel.WARN);
            return "Matches batch written with errors";
        } else {
            return "Matches batch written successfully";
        }
    }
}
