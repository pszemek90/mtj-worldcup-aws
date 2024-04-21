package com.mtjworldcup.getcurrentstatefromapi;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.mtjworldcup.common.model.MatchApiResponse;
import com.mtjworldcup.common.model.MatchDto;
import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.dynamo.model.MatchStatus;
import com.mtjworldcup.getcurrentstatefromapi.service.MatchStateService;
import org.slf4j.Logger;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.mtjworldcup.common.util.Utils.safeGet;
import static org.slf4j.LoggerFactory.*;

public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger log = getLogger(Handler.class);

    private final MatchesDao matchesDao;
    private final MatchStateService matchStateService;

    public Handler() {
        this.matchesDao = new MatchesDao();
        this.matchStateService = new MatchStateService();
    }

    public Handler(MatchesDao matchesDao, MatchStateService matchStateService) {
        this.matchesDao = matchesDao;
        this.matchStateService = matchStateService;
    }


    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            List<Match> unfinishedMatches = matchesDao.getByDate(LocalDate.now())
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(match -> match.getMatchStatus() != MatchStatus.FINISHED)
                    .toList();
            log.info("Unfinished matches: {}", unfinishedMatches);
            List<String> matchIds = unfinishedMatches.stream()
                    .map(Match::getPrimaryId)
                    .filter(Objects::nonNull)
                    .toList();
            MatchApiResponse apiMatches = matchStateService.getCurrentState(matchIds);
            log.info("Current state api response: {}", apiMatches);
            Map<Long, MatchDto> matchesFromApi = Optional.ofNullable(apiMatches)
                    .map(MatchApiResponse::getResponse)
                    .map(dtos -> dtos.stream()
                            .collect(Collectors.toMap(
                                    dto -> Optional.ofNullable(dto)
                                            .map(MatchDto::getFixture)
                                            .map(MatchDto.Fixture::getId)
                                            .orElse(0L),
                                    Function.identity())))
                    .orElse(Collections.emptyMap());
            unfinishedMatches.forEach(match -> {
                MatchDto matchFromApi = matchesFromApi.get(Long.parseLong(match.getPrimaryId()));
                if(matchFromApi == null) {
                    return;
                }
                match.setHomeScore(safeGet(() -> matchFromApi.getGoals().getHome(), null));
                match.setAwayScore(safeGet(() -> matchFromApi.getGoals().getAway(), null));
                match.setMatchStatus(MatchStatus.fromShortName(
                        safeGet(() -> matchFromApi.getFixture().getStatus().getShortName(), "NS")));
                matchesDao.update(match);
            });
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200);
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("Error: " + e.getMessage());
        }
    }
}
