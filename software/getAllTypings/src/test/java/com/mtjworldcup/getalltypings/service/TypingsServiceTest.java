package com.mtjworldcup.getalltypings.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mtjworldcup.common.model.TypingStatus;
import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.getalltypings.model.TypingDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.mtjworldcup.common.model.TypingStatus.CORRECT;
import static com.mtjworldcup.common.model.TypingStatus.INCORRECT;
import static com.mtjworldcup.common.model.TypingStatus.UNKNOWN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TypingsServiceTest {

    private final MatchesDao mockMatchesDao = mock(MatchesDao.class);
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void returnNoTypings_WhenNoTypingsInDb() {
        //given
        TypingsService typingsService = new TypingsService(mockMatchesDao);
        when(mockMatchesDao.getAllTypings()).thenReturn(List.of());
        //when
        var allTypings = typingsService.getAllTypings();
        //then
        assertEquals(0, allTypings.size());
    }

    @Test
    void shouldReturnOneTyping_WhenOneTypingInDb() throws Exception {
        //given
        TypingsService typingsService = new TypingsService(mockMatchesDao);
        LocalDate date = LocalDate.of(2024, 4, 8);
        Match match = prepareMatch(date, "Poland", "Brazil", CORRECT, "user-1");
        when(mockMatchesDao.getAllTypings()).thenReturn(List.of(match));
        //when
        var allTypings = typingsService.getAllTypings();
        //then
        assertEquals(1, allTypings.size());
        assertEquals(1, allTypings.get(date).size());
    }

    @Test
    void shouldReturnTwoTypingsUnderOneDate_WhenTwoTypingsInDb() throws Exception {
        //given
        TypingsService typingsService = new TypingsService(mockMatchesDao);
        LocalDate date = LocalDate.of(2024, 4, 8);
        Match match1 = prepareMatch(date, "Poland", "Brazil", CORRECT, "user-1");
        Match match2 = prepareMatch(date, "Germany", "France", CORRECT, "user-2");
        when(mockMatchesDao.getAllTypings()).thenReturn(List.of(match1, match2));
        //when
        var allTypings = typingsService.getAllTypings();
        //then
        assertEquals(1, allTypings.size());
        assertEquals(2, allTypings.get(date).size());
    }

    @Test
    void shouldReturnTwoTypingsUnderOneDate_WhenThreeTypingsInDbButOneIsUnknown() {
        //given
        TypingsService typingsService = new TypingsService(mockMatchesDao);
        LocalDate date = LocalDate.of(2024, 4, 8);
        Match match1 = prepareMatch(date, "Poland", "Brazil", CORRECT, "user-1");
        Match match2 = prepareMatch(date, "Germany", "France", INCORRECT, "user-2");
        Match match3 = prepareMatch(date, "Italy", "Spain", UNKNOWN, "user-3");
        when(mockMatchesDao.getAllTypings()).thenReturn(List.of(match1, match2, match3));
        //when
        var allTypings = typingsService.getAllTypings();
        //then
        assertEquals(1, allTypings.size());
        assertEquals(2, allTypings.get(date).size());
    }

    @Test
    void shouldReturnNoTypings_WhenExceptionThrown() {
        //given
        TypingsService typingsService = new TypingsService(mockMatchesDao);
        when(mockMatchesDao.getAllTypings()).thenThrow(new RuntimeException("Exception"));
        //when
        var allTypings = typingsService.getAllTypings();
        //then
        assertEquals(Map.of(), allTypings);
    }

    @Test
    void shouldReturnCorrectCombinationOfTypings() {
        //given
        TypingsService typingsService = new TypingsService(mockMatchesDao);
        LocalDate date1 = LocalDate.of(2024, 4, 8);
        LocalDate date2 = LocalDate.of(2024, 4, 9);
        LocalDate date3 = LocalDate.of(2024, 4, 10);
        Match match1 = prepareMatch(date1, "Poland", "Brazil", CORRECT, "user-1");
        Match match8 = prepareMatch(date1, "Poland", "Brazil", CORRECT, "user-2");
        Match match6 = prepareMatch(date1, "England", "Croatia", CORRECT, "user-1");
        Match match7 = prepareMatch(date1, "Uruguay", "Chile", CORRECT, "user-1");
        Match match9 = prepareMatch(date1, "Uruguay", "Chile", INCORRECT, "user-2");
        Match match10 = prepareMatch(date1, "Japan", "South Korea", UNKNOWN, "user-2");
        Match match3 = prepareMatch(date3, "Italy", "Spain", UNKNOWN, "user-1");
        Match match4 = prepareMatch(date3, "Italy", "Spain", UNKNOWN, "user-2");
        Match match2 = prepareMatch(date2, "Germany", "France", CORRECT, "user-2");
        Match match5 = prepareMatch(date2, "Germany", "France", CORRECT, "user-1");
        when(mockMatchesDao.getAllTypings()).thenReturn(List.of(match1, match2, match3, match4, match5, match6, match7, match8, match9, match10));
        //when
        var allTypings = typingsService.getAllTypings();
        //then
        assertEquals(2, allTypings.size());
        assertEquals(3, allTypings.get(date1).size());
        assertEquals(1, allTypings.get(date2).size());
        assertNull(allTypings.get(date3));
    }

    private Match prepareMatch(LocalDate date, String homeTeam, String awayTeam, TypingStatus typingStatus, String user) {
        Match match = new Match();
        match.setDate(date);
        match.setHomeTeam(homeTeam);
        match.setAwayTeam(awayTeam);
        match.setSecondaryId(user);
        match.setHomeScore(2);
        match.setAwayScore(1);
        match.setTypingStatus(typingStatus);
        return match;
    }
}