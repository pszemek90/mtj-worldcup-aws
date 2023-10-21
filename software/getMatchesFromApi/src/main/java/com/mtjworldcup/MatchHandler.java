package com.mtjworldcup;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.mtjworldcup.model.Matches;
import com.mtjworldcup.service.MatchApiService;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

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
        DynamoDbTable<Matches> matches = enhancedClient.table(matchesTableName, TableSchema.fromBean(Matches.class));
        MatchApiService matchService = new MatchApiService();
        matchService.getMatchesFromApi(logger);
        return "hello world";
    }
}
