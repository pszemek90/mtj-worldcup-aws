package com.mtjworldcup.handlefinishedmatch.service;

import com.mtjworldcup.common.model.TypingStatus;
import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.dynamo.model.RecordType;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactUpdateItemEnhancedRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class FinishedMatchService {
  private final MatchesDao matchesDao;
  private final SnsService snsService;

  public FinishedMatchService() {
    this.matchesDao = new MatchesDao();
    this.snsService = new SnsService();
  }

  public FinishedMatchService(MatchesDao matchesDao, SnsService snsService) {
    this.matchesDao = matchesDao;
    this.snsService = snsService;
  }

  public void handleFinishedMatch(String primaryId) {
    List<TransactPutItemEnhancedRequest<Match>> putItemRequests = new ArrayList<>();
    List<TransactUpdateItemEnhancedRequest<Match>> updateItemRequests = new ArrayList<>();
    Match finishedMatch = matchesDao.getById(primaryId);
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
      var updateTomorrowPool = buildUpdateTomorrowPoolRequest(pool);
      updateItemRequests.add(updateTomorrowPool);
    } else {
      correctTypings.forEach(typing -> typing.setTypingStatus(TypingStatus.CORRECT));
      finishedMatch.setCorrectTypings(correctTypings.size());
      List<Match> users =
          correctTypings.stream()
              .map(typing -> matchesDao.getById(typing.getSecondaryId()))
              .toList();
      BigDecimal poolPerUser = pool.divide(BigDecimal.valueOf(users.size()), 2, RoundingMode.DOWN);
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
      snsService.sendMessages(users, finishedMatch, poolPerUser);
    }
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
    matchesDao.transactWriteItems(updateItemRequests, putItemRequests);
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
