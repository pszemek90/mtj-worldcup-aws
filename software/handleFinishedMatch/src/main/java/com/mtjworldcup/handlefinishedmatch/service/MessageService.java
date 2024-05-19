package com.mtjworldcup.handlefinishedmatch.service;

import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.sns.SnsService;
import java.math.BigDecimal;
import java.util.List;

import com.mtjworldcup.sns.exception.UnsubscribedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageService {

  private static final Logger log = LoggerFactory.getLogger(MessageService.class);

  private final SnsService snsService;
  private final MatchesDao matchesDao;

  public MessageService() {
    this.snsService = new SnsService();
    this.matchesDao = new MatchesDao();
  }

  public MessageService(SnsService snsService, MatchesDao matchesDao) {
    this.snsService = snsService;
    this.matchesDao = matchesDao;
  }

  public void sendMessages(List<Match> users, Match finishedMatch, BigDecimal poolPerUser) {
    users.forEach(
        user -> {
          try {
            String endpointArn = snsService.retrieveEndpointArn(user.getFcmToken(), user.getEndpointArn(), user.getPrimaryId());
            storeEndpointArn(endpointArn, user);
            snsService.subscribeToTopic(endpointArn);
            snsService.publishMessage(endpointArn, prepareMessage(finishedMatch, poolPerUser));
          } catch (UnsubscribedException e) {
            log.info("User {} is unsubscribed. Hence not sending message.", user.getPrimaryId());
          } catch (Exception e) {
            log.error("Failed to send message to user: {}", user.getPrimaryId(), e);
          }
        });
  }

  private String prepareMessage(Match finishedMatch, BigDecimal poolPerUser) {
    return String.format(
            """
            {
              "GCM": "{ \\"data\\": { \\"title\\": \\"Wygrana!\\", \\"body\\": \\"Wygrana %.2fzł w meczu %s - %s\\" } }"
            }
            """,
            poolPerUser, finishedMatch.getHomeTeam(), finishedMatch.getAwayTeam());
  }

  private void storeEndpointArn(String endpointArn, Match user) {
    log.info("Storing endpointArn {} for user {}", endpointArn, user.getPrimaryId());
    user.setEndpointArn(endpointArn);
    matchesDao.update(user);
  }
}
