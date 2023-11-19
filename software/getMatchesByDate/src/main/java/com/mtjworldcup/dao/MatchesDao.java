package com.mtjworldcup.dao;

import com.mtjworldcup.mapper.MatchMapper;
import com.mtjworldcup.model.Match;
import com.mtjworldcup.model.MatchDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
        Stream<Match> matchesFromDb = getItemsFromDb(matches, matchDay);
        return matchesFromDb
                .map(MatchMapper::mapToDto)
                .toList();
    }

    Stream<Match> getItemsFromDb(DynamoDbTable<Match> table, LocalDate matchDay) {
        return table.index("getByDate").query(QueryEnhancedRequest.builder()
                        .queryConditional(QueryConditional.keyEqualTo(Key.builder()
                                .partitionValue(matchDay.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                                .build()))
                        .build())
                .stream()
                .flatMap(page -> page.items().stream());
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
