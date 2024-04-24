package com.mtjworldcup.dividepool;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.mtjworldcup.dynamo.dao.MatchesDao;
import com.mtjworldcup.dynamo.model.Match;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HandlerTest {

    @Captor
    private ArgumentCaptor<Match> matchCaptor;
    private AutoCloseable closeable;

    private final MatchesDao mockMatchesDao = mock(MatchesDao.class);
    private Handler handler;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        handler = new Handler(mockMatchesDao);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void shouldSplitThePoolEvenly_WhenThereAreMatchesToBePlayed() {
        //given
        Match todayPool = new Match();
        todayPool.setPool(new BigDecimal(100));
        when(mockMatchesDao.getPool(LocalDate.now())).thenReturn(Optional.of(todayPool));
        Match match1 = new Match();
        match1.setPool(new BigDecimal(0));
        Match match2 = new Match();
        match2.setPool(new BigDecimal(0));
        when(mockMatchesDao.getByDate(any(LocalDate.class))).thenReturn(List.of(match1, match2));
        //when
        APIGatewayProxyResponseEvent response = handler.handleRequest(null, null);
        //then
        assertEquals(200, response.getStatusCode());
        verify(mockMatchesDao, times(2)).update(matchCaptor.capture());
        List<Match> matches = matchCaptor.getAllValues();
        assertEquals(50, matches.get(0).getPool().intValue());
        assertEquals(50, matches.get(1).getPool().intValue());
    }

    @Test
    void shouldNotInvokeGetByDate_WhenTodayPoolIsEmpty() {
        //given
        when(mockMatchesDao.getPool(LocalDate.now())).thenReturn(Optional.empty());
        //when
        APIGatewayProxyResponseEvent response = handler.handleRequest(null, null);
        //then
        assertEquals(200, response.getStatusCode());
        verify(mockMatchesDao, times(0)).getByDate(any(LocalDate.class));
        verify(mockMatchesDao, times(0)).update(any(Match.class));
    }

    @Test
    void shouldNotInvokeGetByDate_WhenTodayPoolIsZero() {
        //given
        Match todayPool = new Match();
        todayPool.setPool(new BigDecimal(0));
        when(mockMatchesDao.getPool(LocalDate.now())).thenReturn(Optional.of(todayPool));
        //when
        APIGatewayProxyResponseEvent response = handler.handleRequest(null, null);
        //then
        assertEquals(200, response.getStatusCode());
        verify(mockMatchesDao, times(0)).getByDate(any(LocalDate.class));
        verify(mockMatchesDao, times(0)).update(any(Match.class));
    }

    @Test
    void shouldNotInvokeUpdate_WhenTodayMatchesIsEmpty() {
        //given
        Match todayPool = new Match();
        todayPool.setPool(new BigDecimal(100));
        when(mockMatchesDao.getPool(LocalDate.now())).thenReturn(Optional.of(todayPool));
        when(mockMatchesDao.getByDate(LocalDate.now())).thenReturn(List.of());
        //when
        APIGatewayProxyResponseEvent response = handler.handleRequest(null, null);
        //then
        assertEquals(200, response.getStatusCode());
        verify(mockMatchesDao, times(0)).update(any(Match.class));
    }

    @Test
    void shouldRoundPoolPerMatchDown_WhenPoolIsNotEvenlyDivided() {
        //given
        Match todayPool = new Match();
        todayPool.setPool(new BigDecimal(200));
        when(mockMatchesDao.getPool(LocalDate.now())).thenReturn(Optional.of(todayPool));
        Match match1 = new Match();
        match1.setPool(new BigDecimal(0));
        Match match2 = new Match();
        match2.setPool(new BigDecimal(0));
        Match match3 = new Match();
        match3.setPool(new BigDecimal(0));
        when(mockMatchesDao.getByDate(any(LocalDate.class))).thenReturn(List.of(match1, match2, match3));
        //when
        APIGatewayProxyResponseEvent response = handler.handleRequest(null, null);
        //then
        assertEquals(200, response.getStatusCode());
        verify(mockMatchesDao, times(3)).update(matchCaptor.capture());
        List<Match> matches = matchCaptor.getAllValues();
        assertEquals(new BigDecimal(66.66, new MathContext(4)), matches.get(0).getPool());
        assertEquals(new BigDecimal(66.66, new MathContext(4)), matches.get(1).getPool());
        assertEquals(new BigDecimal(66.66, new MathContext(4)), matches.get(2).getPool());
    }

    @Test
    void shouldAddPoolToNextDayPool_WhenNoMatchesToday() {
        //given
        Match todayPool = new Match();
        todayPool.setPool(new BigDecimal(100));
        when(mockMatchesDao.getPool(LocalDate.now())).thenReturn(Optional.of(todayPool));
        when(mockMatchesDao.getByDate(LocalDate.now())).thenReturn(List.of());
        Match nextDayPool = new Match();
        nextDayPool.setPool(new BigDecimal(0));
        when(mockMatchesDao.getPool(LocalDate.now().plusDays(1))).thenReturn(Optional.of(nextDayPool));
        //when
        APIGatewayProxyResponseEvent response = handler.handleRequest(null, null);
        //then
        assertEquals(200, response.getStatusCode());
        verify(mockMatchesDao).update(nextDayPool);
        assertEquals(100, nextDayPool.getPool().intValue());
    }
}