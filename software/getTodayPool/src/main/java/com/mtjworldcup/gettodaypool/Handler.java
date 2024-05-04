package com.mtjworldcup.gettodaypool;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.dynamo.model.Match;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.Map;

public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    public static final Logger log = org.slf4j.LoggerFactory.getLogger(Handler.class);

    private final MatchesDao matchesDao;

    public Handler() {
        matchesDao = new MatchesDao();
    }

    public Handler(MatchesDao matchesDao) {
        this.matchesDao = matchesDao;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            Match overallPoolRecord = matchesDao.getTodayPool();
            BigDecimal overallPool = overallPoolRecord.getPool();
            return new APIGatewayProxyResponseEvent()
                    .withBody(String.valueOf(overallPool))
                    .withHeaders(Map.of("Access-Control-Allow-Origin", "http://localhost:5173"))
                    .withStatusCode(200);
        } catch (Exception e) {
            log.error("Error getting overall pool. Cause: {}", e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withBody("Nie udało się pobrać puli. Spróbuj ponownie później.")
                    .withStatusCode(404);
        }
    }
}
