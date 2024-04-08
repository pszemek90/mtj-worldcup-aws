package com.mtjworldcup.getalltypings.service;

import com.mtjworldcup.common.model.TypingStatus;
import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.getalltypings.mapper.TypingMapper;
import com.mtjworldcup.getalltypings.model.TypingDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static java.util.Comparator.reverseOrder;
import static java.util.stream.Collectors.groupingBy;

public class TypingsService {

    public static final Logger log = LoggerFactory.getLogger(TypingsService.class);

    private final MatchesDao matchesDao;

    public TypingsService() {
        this(new MatchesDao());
    }

    public TypingsService(MatchesDao matchesDao) {
        this.matchesDao = matchesDao;
    }

    public Map<LocalDate, Map<String, Set<TypingDto>>> getAllTypings() {
        try {
            List<Match> allTypings = matchesDao.getAllTypings();
            List<Match> typingsForFinishedMatches = allTypings.stream()
                    .filter(match -> match.getTypingStatus() != TypingStatus.UNKNOWN)
                    .toList();
            var typingDtos = TypingMapper.toTypingDto(typingsForFinishedMatches);
            return typingDtos.stream()
                    .collect(
                            groupingBy(
                                    TypingDto::date,
                                    () -> new TreeMap<>(reverseOrder()),
                                    groupingBy(
                                            TypingDto::match,
                                            Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(TypingDto::user)))
                                    )
                            )
                    );
        } catch (Exception e) {
            log.error("Error while getting all typings. Cause: {}", e.getMessage());
            return Map.of();
        }
    }
}
