package com.mtjworldcup.dividepool;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.dynamo.model.RecordType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger log = LoggerFactory.getLogger(Handler.class);

    private final MatchesDao matchesDao;

    public Handler() {
        this.matchesDao = new MatchesDao();
    }

    public Handler(MatchesDao matchesDao) {
        this.matchesDao = matchesDao;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {

            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);
            Match tomorrowPool = createTomorrowPool(tomorrow);
            matchesDao.saveIfNotExists(List.of(tomorrowPool));
            Optional<Match> todayPool = matchesDao.getPool(today);
            if (todayPool.isEmpty()) {
                log.info("No pool to divide");
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(200)
                        .withBody("No pool to divide");
            }
            BigDecimal pool = todayPool.get().getPool();
            if(pool.compareTo(BigDecimal.ZERO) == 0) {
                log.info("No pool from previous matches to divide.");
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(200)
                        .withBody("No pool from previous matches to divide.");
            }
            List<Match> todayMatches = matchesDao.getByDate(today);
            if(todayMatches.isEmpty()) {
                log.info("No matches to divide the pool. Adding pool to the next day pool.");
                matchesDao.getPool(today.plusDays(1)).ifPresent(nextDayPool -> {
                    nextDayPool.setPool(nextDayPool.getPool().add(pool));
                    matchesDao.update(nextDayPool);
                });
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(200)
                        .withBody("No matches to divide the pool.");
            }
            BigDecimal poolPerMatch = pool.divide(new BigDecimal(todayMatches.size()), 2, RoundingMode.DOWN);
            todayMatches.forEach(match -> {
                match.setPool(match.getPool().add(poolPerMatch));
                matchesDao.update(match);
            });
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody("Pool divided successfully");
        } catch (Exception e) {
            log.error("Error dividing the pool. Cause: {}", e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("Error dividing the pool");
        }
    }

    private Match createTomorrowPool(LocalDate tomorrow) {
        Match pool = new Match();
        pool.setDate(tomorrow);
        pool.setPool(BigDecimal.ZERO);
        pool.setPrimaryId("pool-" + tomorrow);
        pool.setSecondaryId("pool-" + tomorrow);
        pool.setRecordType(RecordType.POOL);
        return pool;
    }
}
