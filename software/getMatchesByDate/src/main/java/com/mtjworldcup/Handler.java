package com.mtjworldcup;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.mtjworldcup.dao.MatchesDao;
import com.mtjworldcup.model.MatchDto;
import com.mtjworldcup.model.Matches;
import com.mtjworldcup.model.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

public class Handler implements RequestHandler<Request, Matches> {

    private static final Logger log = LoggerFactory.getLogger(Handler.class);

    private final MatchesDao matchesDao;

    public Handler() {
        this(new MatchesDao());
    }

    public Handler(MatchesDao matchesDao) {
        this.matchesDao = matchesDao;
    }

    @Override
    public Matches handleRequest(Request input, Context context) {
        log.info("Getting matches for date: {}", input);
        List<MatchDto> matchesFromDatabase = matchesDao.getMatchesFromDatabase(input.date());
        log.debug("Matches fetched from database: {}", matchesFromDatabase);
        Matches matches = new Matches();
        matches.setMatches(matchesFromDatabase);
        return matches;
    }
}
