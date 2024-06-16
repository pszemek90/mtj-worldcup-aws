package com.mtjworldcup.handlefinishedmatch.service;

import com.mtjworldcup.common.model.TypingStatus;
import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.dynamo.model.RecordType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactUpdateItemEnhancedRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class FinishedMatchService {

  private static final Logger log = LoggerFactory.getLogger(FinishedMatchService.class);

  private final MatchesDao matchesDao;
  private final MessageService messageService;

  public FinishedMatchService() {
    this.matchesDao = new MatchesDao();
    this.messageService = new MessageService();
  }

  public FinishedMatchService(MatchesDao matchesDao, MessageService messageService) {
    this.matchesDao = matchesDao;
    this.messageService = messageService;
  }

  public void handleFinishedMatch(String primaryId) {
    log.info("Handling finished with id: {}", primaryId);
    List<TransactPutItemEnhancedRequest<Match>> putItemRequests = new ArrayList<>();
    List<TransactUpdateItemEnhancedRequest<Match>> updateItemRequests = new ArrayList<>();
    Match finishedMatch = matchesDao.getById(primaryId);
    log.info("Finished match fetched from DB: {}", finishedMatch);
    BigDecimal pool = finishedMatch.getPool();
    List<Match> typings = matchesDao.getTypingsByMatchId(primaryId);
    typings.forEach(typing -> typing.setTypingStatus(TypingStatus.INCORRECT));
    List<Match> correctTypings =
        typings.stream()
            .filter(
                typing ->
                    typing.getHomeScore().equals(finishedMatch.getHomeScore())
                        && typing.getAwayScore().equals(finishedMatch.getAwayScore()))
            .toList();
    if (correctTypings.isEmpty()) {
      log.info("No correct typing for match: {}", primaryId);
      var updateTomorrowPool = buildUpdateTomorrowPoolRequest(pool);
      updateItemRequests.add(updateTomorrowPool);
    } else {
      log.info("Number of correct typings for match with id: {}: {}", primaryId, correctTypings.size());
      correctTypings.forEach(typing -> typing.setTypingStatus(TypingStatus.CORRECT));
      finishedMatch.setCorrectTypings(correctTypings.size());
      List<Match> users =
          correctTypings.stream()
              .map(typing -> matchesDao.getById(typing.getSecondaryId()))
              .toList();
      log.info("Users with correct typings: {}", users);
      BigDecimal poolPerUser = pool.divide(BigDecimal.valueOf(users.size()), 2, RoundingMode.DOWN);
      log.info("Pool per user calculated: {}", poolPerUser);
      users.forEach(user -> user.setCorrectTypings(user.getCorrectTypings() + 1));
      users.forEach(user -> user.setPool(user.getPool().add(poolPerUser)));
      List<Match> winMessages = new ArrayList<>();
      users.forEach(
          user -> {
            Match message = new Match();
            message.setPrimaryId(user.getPrimaryId());
            message.setSecondaryId("message-" + finishedMatch.getPrimaryId());
            message.setRecordType(RecordType.MESSAGE);
            message.setDate(LocalDate.now());
            message.setPool(poolPerUser);
            message.setHomeTeam(finishedMatch.getHomeTeam());
            message.setAwayTeam(finishedMatch.getAwayTeam());
            winMessages.add(message);
          });
      log.info("Win messages to save: {}", winMessages);
      var putMessagesRequests =
          winMessages.stream()
              .map(
                  message ->
                      TransactPutItemEnhancedRequest.builder(Match.class).item(message).build())
              .toList();
      putItemRequests.addAll(putMessagesRequests);
      var userUpdateRequests =
          users.stream()
              .map(
                  user -> TransactUpdateItemEnhancedRequest.builder(Match.class).item(user).build())
              .toList();
      updateItemRequests.addAll(userUpdateRequests);
      messageService.sendMessages(users, finishedMatch, poolPerUser);
    }
    finishedMatch.setDisplayPool(pool);
    finishedMatch.setPool(BigDecimal.ZERO);
    var updateMatch =
        TransactUpdateItemEnhancedRequest.builder(Match.class).item(finishedMatch).build();
    var typingsUpdateRequests =
        typings.stream()
            .map(
                typing ->
                    TransactUpdateItemEnhancedRequest.builder(Match.class).item(typing).build())
            .toList();
    updateItemRequests.add(updateMatch);
    updateItemRequests.addAll(typingsUpdateRequests);
    log.info("Put item requests: {}", putItemRequests);
    log.info("Update item requests: {}", updateMatch);
    matchesDao.transactWriteItems(updateItemRequests, putItemRequests);
    log.info("Transaction successful for match id: {}", primaryId);
  }

  public TransactUpdateItemEnhancedRequest<Match> buildUpdateTomorrowPoolRequest(BigDecimal pool) {
    Match tomorrowPool =
        matchesDao
            .getPool(LocalDate.now().plusDays(1))
            .orElseThrow(() -> new NoSuchElementException("Tomorrow pool not found!"));
    tomorrowPool.setPool(tomorrowPool.getPool().add(pool));
    return TransactUpdateItemEnhancedRequest.builder(Match.class).item(tomorrowPool).build();
  }
}
