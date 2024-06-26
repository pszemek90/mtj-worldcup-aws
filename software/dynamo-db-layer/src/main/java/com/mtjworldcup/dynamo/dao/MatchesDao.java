package com.mtjworldcup.dynamo.dao;

import static software.amazon.awssdk.regions.Region.EU_CENTRAL_1;

import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.dynamo.model.MatchStatus;
import com.mtjworldcup.dynamo.model.RecordType;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

public class MatchesDao {

  private static final Logger log = LoggerFactory.getLogger(MatchesDao.class);

  private static final String GET_BY_SECONDARY_ID_INDEX = "getBySecondaryId";
  private static final String GET_BY_DATE_INDEX = "getByDate";
  private static final String GET_BY_RECORD_TYPE_INDEX = "getByRecordType";

  private final DynamoDbClient dynamoClient;
  private final DynamoDbEnhancedClient enhancedClient;

  public MatchesDao() {
    boolean isLocal = System.getenv("AWS_SAM_LOCAL") != null;
    this.dynamoClient = prepareClient(isLocal);
    this.enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoClient).build();
  }

  public MatchesDao(DynamoDbClient dynamoClient, DynamoDbEnhancedClient enhancedClient) {
    this.dynamoClient = dynamoClient;
    this.enhancedClient = enhancedClient;
  }

  public List<Match> getFinishedMatches() {
    DynamoDbTable<Match> matchesTable = getMatchTable();
    return matchesTable
        .index(GET_BY_RECORD_TYPE_INDEX)
        .query(
            QueryEnhancedRequest.builder()
                .queryConditional(
                    QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(RecordType.MATCH.name()).build()))
                .filterExpression(
                    Expression.builder()
                        .expression("#matchStatus = :matchStatus")
                        .putExpressionName("#matchStatus", "match_status")
                        .putExpressionValue(
                            ":matchStatus",
                            AttributeValue.builder().s(MatchStatus.FINISHED.name()).build())
                        .build())
                .build())
        .stream()
        .flatMap(page -> page.items().stream())
        .toList();
  }

  public List<Match> getByDate(LocalDate matchDay) {
    log.debug("Getting matches for match date: {}", matchDay);
    var matches = getMatchTable();
    return matches
        .index(GET_BY_DATE_INDEX)
        .query(
            QueryEnhancedRequest.builder()
                .queryConditional(
                    QueryConditional.keyEqualTo(
                        Key.builder()
                            .partitionValue(
                                matchDay.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                            .build()))
                .filterExpression(filterByType(RecordType.MATCH))
                .build())
        .stream()
        .flatMap(page -> page.items().stream())
        .toList();
  }

  public Optional<Match> getPool(LocalDate poolDate) {
    DynamoDbTable<Match> matchTable = getMatchTable();
    return matchTable
        .index(GET_BY_DATE_INDEX)
        .query(
            QueryEnhancedRequest.builder()
                .queryConditional(
                    QueryConditional.keyEqualTo(
                        Key.builder()
                            .partitionValue(
                                poolDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                            .build()))
                .filterExpression(filterByType(RecordType.POOL))
                .build())
        .stream()
        .flatMap(page -> page.items().stream())
        .findFirst();
  }

  public Match getById(String id) {
    var matches = getMatchTable();
    return matches.getItem(
        GetItemEnhancedRequest.builder()
            .key(builder -> builder.partitionValue(id).sortValue(id))
            .build());
  }

  public void saveIfNotExists(List<Match> filteredEntities) {
    if (filteredEntities == null) {
      throw new IllegalStateException("Attempt to save null list of entities");
    }
    log.info("Saving {} records to DB", filteredEntities.size());
    DynamoDbTable<Match> matchTable = getMatchTable();
    filteredEntities.forEach(
        entity -> {
          try {
            matchTable.putItem(
                builder ->
                    builder
                        .item(entity)
                        .conditionExpression(
                            Expression.builder()
                                .expression(
                                    "attribute_not_exists(primary_id) AND attribute_not_exists(secondary_id)")
                                .build()));
          } catch (Exception e) {
            log.warn(
                "Entity was not persisted correctly. Entity: {}. Cause: {}",
                entity,
                e.getMessage());
          }
        });
  }

  public void saveTypings(List<Match> typings) {
    if (typings == null) {
      throw new IllegalStateException("Attempt to save null list of typings");
    }
    log.info("Saving {} typings to DB", typings.size());
    DynamoDbTable<Match> matchTable = getMatchTable();
    typings.forEach(
        typing -> {
          Match existingTyping = getByCombinedKey(typing.getPrimaryId(), typing.getSecondaryId());
          if (existingTyping != null) {
            matchTable.putItem(builder -> builder.item(typing));
          } else {
            Match match = getById(typing.getPrimaryId());
            if (match == null)
              throw new NoSuchElementException("Match not found for id: " + typing.getPrimaryId());
            match.setPool(match.getPool().add(BigDecimal.ONE));
            Match user = getById(typing.getSecondaryId());
            if (user == null)
              throw new NoSuchElementException("User not found for id: " + typing.getSecondaryId());
            user.setPool(user.getPool().subtract(BigDecimal.ONE));
            var putTypingRequest =
                TransactPutItemEnhancedRequest.builder(Match.class).item(typing).build();
            var updateMatchPool =
                TransactUpdateItemEnhancedRequest.builder(Match.class).item(match).build();
            var updateUserPool =
                TransactUpdateItemEnhancedRequest.builder(Match.class).item(user).build();
            var transactionRequest =
                TransactWriteItemsEnhancedRequest.builder()
                    .addPutItem(matchTable, putTypingRequest)
                    .addUpdateItem(matchTable, updateMatchPool)
                    .addUpdateItem(matchTable, updateUserPool)
                    .build();
            try {
              enhancedClient.transactWriteItems(transactionRequest);
            } catch (TransactionCanceledException e) {
              log.info(
                  "Transaction cancelled. User: {}. Cause: {}",
                  user.getSecondaryId(),
                  e.getMessage());
            }
          }
        });
  }

  public List<Match> getTypingsByUserId(String userId) {
    DynamoDbTable<Match> matchTable = getMatchTable();
    return matchTable
        .index(GET_BY_SECONDARY_ID_INDEX)
        .query(
            QueryEnhancedRequest.builder()
                .queryConditional(
                    QueryConditional.keyEqualTo(Key.builder().partitionValue(userId).build()))
                .filterExpression(filterByType(RecordType.TYPING))
                .build())
        .stream()
        .flatMap(page -> page.items().stream())
        .toList();
  }

  public List<Match> getTypingsByMatchId(String userId) {
    DynamoDbTable<Match> matchTable = getMatchTable();
    return matchTable
        .query(
            QueryEnhancedRequest.builder()
                .queryConditional(
                    QueryConditional.keyEqualTo(Key.builder().partitionValue(userId).build()))
                .filterExpression(filterByType(RecordType.TYPING))
                .build())
        .stream()
        .flatMap(page -> page.items().stream())
        .toList();
  }

  public List<Match> getAllTypings() {
    return getByType(RecordType.TYPING);
  }

  public Match getTodayPool() {
    return getPool(LocalDate.now())
            .orElseThrow(() -> new NoSuchElementException("Today pool not found!"));
  }

  public void update(Match entity) {
    log.debug("Updating entity: {}", entity);
    DynamoDbTable<Match> matchTable = getMatchTable();
    try {
      matchTable.updateItem(builder -> builder.item(entity));
    } catch (Exception e) {
      log.warn("Entity was not updated correctly. Entity: {}. Cause: {}", entity, e.getMessage());
    }
  }

  public void transactWriteItems(
          List<TransactUpdateItemEnhancedRequest<Match>> updateRequests,
          List<TransactPutItemEnhancedRequest<Match>> putRequests) {
    DynamoDbTable<Match> matchTable = getMatchTable();
    var transctionBuilder = TransactWriteItemsEnhancedRequest.builder();
    updateRequests.forEach(update -> transctionBuilder.addUpdateItem(matchTable, update));
    putRequests.forEach(put -> transctionBuilder.addPutItem(matchTable, put));
    enhancedClient.transactWriteItems(transctionBuilder.build());
  }

  public List<Match> getUsers() {
    return getByType(RecordType.USER);
  }

  public List<Match> getMessagesByUserId(String userId) {
    DynamoDbTable<Match> matchTable = getMatchTable();
    return matchTable
        .query(
            QueryEnhancedRequest.builder()
                .queryConditional(
                    QueryConditional.keyEqualTo(Key.builder().partitionValue(userId).build()))
                .filterExpression(filterByType(RecordType.MESSAGE))
                .build())
        .stream()
        .flatMap(page -> page.items().stream())
        .toList();
  }

  private List<Match> getByType(RecordType recordType) {
    DynamoDbTable<Match> matchTable = getMatchTable();
    return matchTable
        .index(GET_BY_RECORD_TYPE_INDEX)
        .query(
            QueryEnhancedRequest.builder()
                .queryConditional(
                    QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(recordType.name()).build()))
                .build())
        .stream()
        .flatMap(page -> page.items().stream())
        .toList();
  }

  private Expression filterByType(RecordType recordType) {
    return Expression.builder()
        .expression("#recordType = :recordType")
        .putExpressionName("#recordType", "record_type")
        .putExpressionValue(":recordType", AttributeValue.builder().s(recordType.name()).build())
        .build();
  }

  private DynamoDbTable<Match> getMatchTable() {
    String matchesTableName = System.getenv("MATCHES_TABLE_NAME");
    return enhancedClient.table(matchesTableName, TableSchema.fromBean(Match.class));
  }

  public Match getByCombinedKey(String primaryId, String secondaryId) {
    DynamoDbTable<Match> matchTable = getMatchTable();
    return matchTable.getItem(
        GetItemEnhancedRequest.builder()
            .key(builder -> builder.partitionValue(primaryId).sortValue(secondaryId).build())
            .build());
  }

  private DynamoDbClient prepareClient(boolean isLocal) {
    DynamoDbClientBuilder builder = DynamoDbClient.builder().region(EU_CENTRAL_1);
    return isLocal
        ? builder.endpointOverride(URI.create("http://local-ddb:8000")).build()
        : builder.build();
  }
}
