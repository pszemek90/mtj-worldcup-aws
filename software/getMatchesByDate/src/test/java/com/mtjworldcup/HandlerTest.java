package com.mtjworldcup;

import com.mtjworldcup.dao.MatchesDao;
import com.mtjworldcup.model.MatchDto;
import com.mtjworldcup.model.Matches;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static java.time.Month.OCTOBER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HandlerTest {

    @Test
    void shouldReturnOneMatch_WhenOneMatchForADateAvailable() {
        //given
        MatchesDao mockDao = mock(MatchesDao.class);
        List<MatchDto> matchesFromDate = new ArrayList<>();
        matchesFromDate.add(new MatchDto(123L, LocalDateTime.of(LocalDate.of(2023, OCTOBER, 28), LocalTime.of(15, 0)), "dummyHomeTeam", "dummyAwayTeam"));
        when(mockDao.getMatchesFromDatabase(any())).thenReturn(matchesFromDate);
        Handler handler = new Handler(mockDao);
        //when
        Matches actualMatches = handler.handleRequest(LocalDate.of(2023, OCTOBER, 28), null);
        //then
        assertEquals(1, actualMatches.getMatches().size());
    }

    @Test
    void shouldReturnTwoMatches_WhenTwoMatchesForADateAvailable() {
        //given
        MatchesDao mockDao = mock(MatchesDao.class);
        List<MatchDto> matchesFromDate = new ArrayList<>();
        matchesFromDate.add(new MatchDto(123L, LocalDateTime.of(LocalDate.of(2023, OCTOBER, 28), LocalTime.of(15, 0)), "dummyHomeTeam", "dummyAwayTeam"));
        matchesFromDate.add(new MatchDto(124L, LocalDateTime.of(LocalDate.of(2023, OCTOBER, 28), LocalTime.of(15, 0)), "dummyHomeTeam", "dummyAwayTeam"));
        when(mockDao.getMatchesFromDatabase(any())).thenReturn(matchesFromDate);
        Handler handler = new Handler(mockDao);
        //when
        Matches actualMatches = handler.handleRequest(LocalDate.of(2023, OCTOBER, 28), null);
        //then
        assertEquals(2, actualMatches.getMatches().size());
    }
}