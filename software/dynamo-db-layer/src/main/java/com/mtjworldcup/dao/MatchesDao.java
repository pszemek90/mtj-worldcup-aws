package com.mtjworldcup.dao;

import com.mtjworldcup.model.Match;
import com.mtjworldcup.model.RecordType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

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

    public MatchesDao(DynamoDbClient dynamoClient, DynamoDbEnhancedClient enhancedClient) {
        this.dynamoClient = dynamoClient;
        this.enhancedClient = enhancedClient;
    }

    public List<Match> getFinishedMatches() {
        DynamoDbTable<Match> matchesTable = getMatchTable();
        return matchesTable.index("getByRecordType")
                .query(QueryEnhancedRequest.builder()
                        .queryConditional(QueryConditional
                                .keyEqualTo(Key.builder()
                                        .partitionValue(RecordType.MATCH.name())
                                        .build()))
                        .filterExpression(Expression.builder()
                                .expression("attribute_exists(#awayScore) AND attribute_exists(#homeScore)")
                                .putExpressionName("#awayScore", "away_score")
                                .putExpressionName("#homeScore", "home_score")
                                .build())
                        .build())
                .stream()
                .flatMap(page -> page.items().stream())
                .toList();
    }

    public List<Match> getByDate(LocalDate matchDay) {
        log.debug("Getting matches for match date: {}", matchDay);
        var matches = getMatchTable();
        Stream<Match> matchesFromDb = getByDateIndexFromDb(matches, matchDay);
        return matchesFromDb
                .toList();
    }

    public Match getById(String primaryId) {
        var matches = getMatchTable();
        return matches.getItem(GetItemEnhancedRequest.builder()
                        .key(builder -> builder
                                .partitionValue(primaryId)
                                .sortValue(primaryId)
                                .build())
                .build());
    }

    private DynamoDbTable<Match> getMatchTable() {
        String matchesTableName = System.getenv("MATCHES_TABLE_NAME");
        log.info("Matches table name: {}", matchesTableName);
        return enhancedClient.table(matchesTableName, TableSchema.fromBean(Match.class));
    }

    Stream<Match> getByDateIndexFromDb(DynamoDbTable<Match> table, LocalDate matchDay) {
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

    public BatchWriteResult save(List<Match> filteredEntities) {
        String userId = filteredEntities.get(0).getSecondaryId();
        log.debug("Saving {} types for user {}", filteredEntities.size(), userId);
        DynamoDbTable<Match> matchTable = getMatchTable();
        List<WriteBatch> writeBatches = filteredEntities.stream()
                .map(entity -> WriteBatch.builder(Match.class)
                        .mappedTableResource(matchTable)
                        .addPutItem(entity)
                        .build())
                .toList();
        BatchWriteItemEnhancedRequest batchRequest = BatchWriteItemEnhancedRequest.builder()
                .writeBatches(writeBatches)
                .build();
        return enhancedClient.batchWriteItem(batchRequest);
    }
}
