package com.mtjworldcup.dao;

import com.mtjworldcup.mapper.MatchMapper;
import com.mtjworldcup.model.Match;
import com.mtjworldcup.model.MatchDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;
import static software.amazon.awssdk.regions.Region.EU_CENTRAL_1;

public class MatchesDao {

    private static final Logger log = LoggerFactory.getLogger(MatchesDao.class);

    private final DynamoDbClient dynamoClient;
    private final DynamoDbEnhancedClient enhancedClient;

    public MatchesDao() {
        boolean isLocal = System.getenv("AWS_SAM_LOCAL") != null;
        this.dynamoClient = prepareClient(isLocal);
        this.enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoClient)
                .build();
    }

    public MatchesDao(DynamoDbClient dynamoClient,DynamoDbEnhancedClient enhancedClient) {
        this.dynamoClient = dynamoClient;
        this.enhancedClient = enhancedClient;
    }

    public List<MatchDto> getMatchesFromDatabase(LocalDate matchDay) {
        String matchesTableName = System.getenv("MATCHES_TABLE_NAME");
        log.info("Matches table name: {}", matchesTableName);
        DynamoDbTable<Match> matches = enhancedClient.table(matchesTableName, TableSchema.fromBean(Match.class));
        String startOfDay = matchDay.atStartOfDay().toString();
        String endOfDay = matchDay.atTime(23, 59, 59).toString();
        Stream<Match> matchesFromDb = getItemsFromDb(matches, startOfDay, endOfDay);
        return matchesFromDb
                .map(MatchMapper::mapToDto)
                .toList();
    }

    Stream<Match> getItemsFromDb(DynamoDbTable<Match> table, String startOfDay, String endOfDay) {
        return table.scan(ScanEnhancedRequest.builder()
                        .filterExpression(Expression.builder()
                                .expression("#start_time > :start_day and #start_time < :end_day")
                                .putExpressionName("#start_time", "start_time")
                                .putExpressionValue(":start_day", stringValue(startOfDay))
                                .putExpressionValue(":end_day", stringValue(endOfDay))
                                .build())
                        .build())
                .items()
                .stream();
    }

    private DynamoDbClient prepareClient(boolean isLocal) {
        if(isLocal) {
            return DynamoDbClient.builder()
                    .region(EU_CENTRAL_1)
                    .endpointOverride(URI.create("http://local-ddb:8000"))
                    .build();
        }
        return DynamoDbClient.builder()
                .region(EU_CENTRAL_1)
                .build();
    }
}
