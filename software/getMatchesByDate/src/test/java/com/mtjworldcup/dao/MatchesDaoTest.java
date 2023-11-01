package com.mtjworldcup.dao;

import com.mtjworldcup.model.Match;
import com.mtjworldcup.model.MatchDto;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.time.Month.OCTOBER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MatchesDaoTest {

    @Test
    void shouldReturnOneMatch_WhenOneMatchInDb() {
        //given
        DynamoDbClient mockDynamoDbClient = mock(DynamoDbClient.class);
        DynamoDbEnhancedClient mockEnhancedClient = mock(DynamoDbEnhancedClient.class);
        MatchesDao matchesDao = new MatchesDao(mockDynamoDbClient, mockEnhancedClient);
        LocalDate matchDate = LocalDate.of(2023, OCTOBER, 29);
        Stream<Match> matchStream = prepareMatches(1);
        MatchesDao matchesDaoSpy = spy(matchesDao);
        doReturn(matchStream).when(matchesDaoSpy).getItemsFromDb(any(), any(), any());
        //when
        List<MatchDto> matchesFromDatabase = matchesDaoSpy.getMatchesFromDatabase(matchDate);
        //then
        assertEquals(1, matchesFromDatabase.size());
    }

    @Test
    void shouldReturnTwoMatches_WhenTwoMatchesInDb() {
        //given
        DynamoDbClient mockDynamoDbClient = mock(DynamoDbClient.class);
        DynamoDbEnhancedClient mockEnhancedClient = mock(DynamoDbEnhancedClient.class);
        MatchesDao matchesDao = new MatchesDao(mockDynamoDbClient, mockEnhancedClient);
        LocalDate matchDate = LocalDate.of(2023, OCTOBER, 29);
        Stream<Match> matchStream = prepareMatches(2);
        MatchesDao matchesDaoSpy = spy(matchesDao);
        doReturn(matchStream).when(matchesDaoSpy).getItemsFromDb(any(), any(), any());
        //when
        List<MatchDto> matchesFromDatabase = matchesDaoSpy.getMatchesFromDatabase(matchDate);
        //then
        assertEquals(2, matchesFromDatabase.size());
    }

    private Stream<Match> prepareMatches(int numberOfMatches) {
        ArrayList<Match> matches = new ArrayList<>();
        for (int i = 0; i < numberOfMatches; i++) {
            Match match = new Match();
            match.setAwayScore(i % 4);
            match.setHomeScore(i % 2);
            match.setMatchId((long) i);
            match.setStartTime(LocalDateTime.of(2023, OCTOBER, 29, i % 23, i % 59));
            match.setAwayTeam("team" + (i + 1));
            match.setHomeTeam("team" + (i + 2));
            matches.add(match);
        }
        return matches.stream();
    }
}