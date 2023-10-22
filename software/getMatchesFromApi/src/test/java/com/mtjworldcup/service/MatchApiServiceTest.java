package com.mtjworldcup.service;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtjworldcup.model.MatchApiResponse;
import com.mtjworldcup.model.MatchDto;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MatchApiServiceTest {

    private static MockWebServer mockWebServer;
    private static LambdaLogger logger;
    private static OkHttpClient okHttpClient;
    private static ObjectMapper objectMapper;
    private static String baseUrl;

    @BeforeAll
    static void setupOnce() throws Exception{
        logger = new LambdaLogger() {
            @Override
            public void log(String message) {
                System.out.println(message);
            }

            @Override
            public void log(byte[] message) {
                //no op
            }
        };
        okHttpClient = new OkHttpClient();
        objectMapper = new ObjectMapper();
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        baseUrl = mockWebServer.url("/").toString();
    }

    @Test
    void shouldReturnValidDtos_WhenCallWasSuccessful() throws Exception{
        //given
        List<MatchDto> matches = List.of(new MatchDto());
        MatchApiResponse matchApiResponse = new MatchApiResponse();
        matchApiResponse.setResponse(matches);
        String matchesAsString = objectMapper.writeValueAsString(matchApiResponse);
        MockResponse mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(matchesAsString);
        mockWebServer.enqueue(mockResponse);
        MatchApiService matchApiService = new MatchApiService(logger, okHttpClient);
        MatchApiService matchApiServiceSpy = spy(matchApiService);
        doReturn("TEST").when(matchApiServiceSpy).getEnvironmentVariable("RAPID_API_KEY");
        //when
        List<MatchDto> actualMatchesFromApi = matchApiServiceSpy.getMatchesFromApi(baseUrl);
        //then
        int expectedMatchesFromApiSize = 1;
        assertEquals(expectedMatchesFromApiSize, actualMatchesFromApi.size());
    }

    @Test
    void shouldThrowNoSuchElementException_WhenNoRapidApiKeyVariablePresent() {
        //given
        MatchApiService matchApiService = new MatchApiService(logger, okHttpClient);
        //when, then
        assertThrows(NoSuchElementException.class, () -> matchApiService.getMatchesFromApi(baseUrl));
    }
}