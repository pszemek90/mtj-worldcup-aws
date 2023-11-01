package com.mtjworldcup.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtjworldcup.model.MatchApiResponse;
import com.mtjworldcup.model.MatchDto;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SystemStubsExtension.class)
class MatchApiServiceTest {

    @SystemStub
    private EnvironmentVariables environmentVariables;

    private static MockWebServer mockWebServer;
    private static OkHttpClient okHttpClient;
    private static ObjectMapper objectMapper;
    private static String baseUrl;

    @BeforeAll
    static void setupOnce() throws Exception{
        okHttpClient = new OkHttpClient();
        objectMapper = new ObjectMapper();
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        baseUrl = mockWebServer.url("/").toString();
    }

    @Test
    void shouldReturnValidDtos_WhenCallWasSuccessful() throws Exception{
        //given
        environmentVariables.set("RAPID_API_KEY", "TEST");
        List<MatchDto> matches = List.of(new MatchDto());
        MatchApiResponse matchApiResponse = new MatchApiResponse();
        matchApiResponse.setResponse(matches);
        String matchesAsString = objectMapper.writeValueAsString(matchApiResponse);
        MockResponse mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(matchesAsString);
        mockWebServer.enqueue(mockResponse);
        MatchApiService matchApiService = new MatchApiService(okHttpClient);
        //when
        List<MatchDto> actualMatchesFromApi = matchApiService.getMatchesFromApi(baseUrl);
        //then
        int expectedMatchesFromApiSize = 1;
        assertEquals(expectedMatchesFromApiSize, actualMatchesFromApi.size());
    }

    @Test
    void shouldThrowNoSuchElementException_WhenNoRapidApiKeyVariablePresent() {
        //given
        MatchApiService matchApiService = new MatchApiService(okHttpClient);
        //when, then
        assertThrows(NullPointerException.class, () -> matchApiService.getMatchesFromApi(baseUrl));
    }
}