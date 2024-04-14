package com.mtjworldcup.dynamo.dao;

import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.dynamo.model.MatchStatus;
import com.mtjworldcup.dynamo.model.RecordType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;

import static software.amazon.awssdk.regions.Region.EU_CENTRAL_1;

public class MatchesDao {

    private static final Logger log = LoggerFactory.getLogger(MatchesDao.class);

    private static final String GET_BY_SECONDARY_ID_INDEX = "getBySecondaryId";
    private static final String GET_BY_DATE_INDEX = "getByDate";
    private static final String GET_BY_RECORD_TYPE_INDEX = "getByRecordType";
    private static final String OVERALL_POOL = "overall_pool";

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
        return matchesTable.index(GET_BY_RECORD_TYPE_INDEX)
                .query(QueryEnhancedRequest.builder()
                        .queryConditional(QueryConditional
                                .keyEqualTo(Key.builder()
                                        .partitionValue(RecordType.MATCH.name())
                                        .build()))
                        .filterExpression(Expression.builder()
                                .expression("#matchStatus = :matchStatus")
                                .putExpressionName("#matchStatus", "match_status")
                                .putExpressionValue(":matchStatus", AttributeValue.builder()
                                        .s(MatchStatus.FINISHED.name())
                                        .build())
                                .build())
                        .build())
                .stream()
                .flatMap(page -> page.items().stream())
                .toList();
    }

    public List<Match> getByDate(LocalDate matchDay) {
        log.debug("Getting matches for match date: {}", matchDay);
        var matches = getMatchTable();
        return matches.index(GET_BY_DATE_INDEX).query(QueryEnhancedRequest.builder()
                        .queryConditional(QueryConditional.keyEqualTo(Key.builder()
                                .partitionValue(matchDay.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                                .build()))
                        .filterExpression(Expression.builder()
                                .expression("#recordType = :recordType")
                                .putExpressionName("#recordType", "record_type")
                                .putExpressionValue(":recordType", AttributeValue.builder()
                                        .s(RecordType.MATCH.name())
                                        .build())
                                .build())
                        .build())
                .stream()
                .flatMap(page -> page.items().stream())
                .toList();
    }

    public Match getById(String id) {
        var matches = getMatchTable();
        return matches.getItem(GetItemEnhancedRequest.builder()
                .key(builder -> builder
                        .partitionValue(id)
                        .sortValue(id))
                .build());
    }

    public DynamoDbTable<Match> getMatchTable() {
        String matchesTableName = System.getenv("MATCHES_TABLE_NAME");
        return enhancedClient.table(matchesTableName, TableSchema.fromBean(Match.class));
    }

    public BatchWriteResult save(List<Match> filteredEntities) {
        if (filteredEntities == null) {
            throw new IllegalStateException("Attempt to save null list of entities");
        }
        log.info("Saving {} records to DB", filteredEntities.size());
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

    public void saveTypings(List<Match> typings) {
        if (typings == null) {
            throw new IllegalStateException("Attempt to save null list of typings");
        }
        log.info("Saving {} typings to DB", typings.size());
        DynamoDbTable<Match> matchTable = getMatchTable();
        typings.forEach(typing -> {
            Match existingTyping = getByCombinedKey(typing.getPrimaryId(), typing.getSecondaryId());
            if (existingTyping != null) {
                matchTable.putItem(builder -> builder.item(typing));
            } else {
                Match match = getById(typing.getPrimaryId());
                if (match == null)
                    throw new NoSuchElementException("Match not found for id: " + typing.getPrimaryId());
                match.setPool(match.getPool() + 1);
                Match user = getById(typing.getSecondaryId());
                if (user == null)
                    throw new NoSuchElementException("User not found for id: " + typing.getSecondaryId());
                user.setPool(user.getPool() - 1);
                var putTypingRequest = TransactPutItemEnhancedRequest.builder(Match.class)
                        .item(typing)
                        .build();
                var updateMatchPool = TransactUpdateItemEnhancedRequest.builder(Match.class)
                        .item(match)
                        .build();
                var updateUserPool = TransactUpdateItemEnhancedRequest.builder(Match.class)
                        .item(user)
                        .build();
                var transactionRequest = TransactWriteItemsEnhancedRequest.builder()
                        .addPutItem(matchTable, putTypingRequest)
                        .addUpdateItem(matchTable, updateMatchPool)
                        .addUpdateItem(matchTable, updateUserPool)
                        .build();
                try {
                    enhancedClient.transactWriteItems(transactionRequest);
                } catch (TransactionCanceledException e) {
                    log.info("Transaction cancelled. User: {}. Cause: {}", user.getSecondaryId(), e.getMessage());
                }
            }
        });
    }

    private Match getByCombinedKey(String primaryId, String secondaryId) {
        DynamoDbTable<Match> matchTable = getMatchTable();
        return matchTable.getItem(GetItemEnhancedRequest.builder()
                .key(builder -> builder
                        .partitionValue(primaryId)
                        .sortValue(secondaryId)
                        .build())
                .build());
    }

    public List<Match> getTypings(String userId) {
        DynamoDbTable<Match> matchTable = getMatchTable();
        return matchTable.index(GET_BY_SECONDARY_ID_INDEX)
                .query(QueryEnhancedRequest.builder()
                        .queryConditional(QueryConditional.keyEqualTo(Key.builder()
                                .partitionValue(userId)
                                .build()))
                        .attributesToProject(List.of(
                                "date",
                                "home_team",
                                "away_team",
                                "home_score",
                                "away_score",
                                "typing_status"))
                        .filterExpression(Expression.builder()
                                .expression("#recordType = :recordType")
                                .putExpressionName("#recordType", "record_type")
                                .putExpressionValue(":recordType", AttributeValue.builder()
                                        .s(RecordType.TYPING.name())
                                        .build())
                                .build())
                        .build())
                .stream()
                .flatMap(page -> page.items().stream())
                .toList();
    }

    public List<Match> getAllTypings() {
        DynamoDbTable<Match> matchTable = getMatchTable();
        return matchTable.index(GET_BY_RECORD_TYPE_INDEX)
                .query(QueryEnhancedRequest.builder()
                        .queryConditional(QueryConditional
                                .keyEqualTo(Key.builder()
                                        .partitionValue(RecordType.TYPING.name())
                                        .build()))
                        .build())
                .stream()
                .flatMap(page -> page.items().stream())
                .toList();
    }

    public Match getOverallPool() {
        DynamoDbTable<Match> matchTable = getMatchTable();
        return matchTable.getItem(GetItemEnhancedRequest.builder()
                .key(builder -> builder
                        .partitionValue(OVERALL_POOL)
                        .sortValue(OVERALL_POOL)
                        .build())
                .build());
    }

    private DynamoDbClient prepareClient(boolean isLocal) {
        if (isLocal) {
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
