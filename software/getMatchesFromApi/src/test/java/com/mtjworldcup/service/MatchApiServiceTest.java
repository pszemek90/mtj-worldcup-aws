package com.mtjworldcup.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtjworldcup.model.*;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SystemStubsExtension.class)
class MatchApiServiceTest {

    @SystemStub
    private EnvironmentVariables environmentVariables;

    private MockWebServer mockWebServer;
    private static OkHttpClient okHttpClient;
    private static ObjectMapper objectMapper;
    private static String baseUrl;

    @BeforeAll
    static void setupOnce() {
        okHttpClient = new OkHttpClient();
        objectMapper = new ObjectMapper();
    }

    @BeforeEach
    void setUp() throws Exception{
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        baseUrl = mockWebServer.url("/").toString();
    }

    @AfterEach
    void tearDown() throws Exception{
        mockWebServer.shutdown();
    }

    @Test
    void shouldReturnValidDtos_WhenCallWasSuccessful() throws Exception {
        //given
        environmentVariables.set("RAPID_API_KEY", "TEST");
        String currentSeasonResponse = Files.readString(Path.of("src/test/resources/files/successful-get-seasons-response.json"));
        MockResponse mockCurrentSeasonResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(currentSeasonResponse);
        mockWebServer.enqueue(mockCurrentSeasonResponse);
        String matchesAsString = Files.readString(Path.of("src/test/resources/files/successful-get-matches-response.json"));
        MockResponse mockGetMatchesResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(matchesAsString);
        mockWebServer.enqueue(mockGetMatchesResponse);
        MatchApiService matchApiService = new MatchApiService(okHttpClient, objectMapper);
        //when
        List<MatchDto> actualMatchesFromApi = matchApiService.getMatchesFromApi(baseUrl);
        //then
        int expectedMatchesFromApiSize = 17;
        assertEquals(expectedMatchesFromApiSize, actualMatchesFromApi.size());
    }

    @Test
    void shouldNotInvokeGetMatches_WhenNoSeasonFound() throws Exception {
        //given
        environmentVariables.set("RAPID_API_KEY", "TEST");
        String currentSeasonResponse = Files.readString(Path.of("src/test/resources/files/failure-get-seasons-response.json"));
        MockResponse mockCurrentSeasonResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(currentSeasonResponse);
        mockWebServer.enqueue(mockCurrentSeasonResponse);
        String matchesAsString = Files.readString(Path.of("src/test/resources/files/successful-get-matches-response.json"));
        MockResponse mockGetMatchesResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(matchesAsString);
        mockWebServer.enqueue(mockGetMatchesResponse);
        MatchApiService matchApiService = new MatchApiService(okHttpClient, objectMapper);
        //when, then
        assertThrows(NoSuchElementException.class, () -> matchApiService.getMatchesFromApi(baseUrl));
        assertEquals(1, mockWebServer.getRequestCount());
    }

    @Test
    void shouldThrowNoSuchElementException_WhenNoRapidApiKeyVariablePresent() {
        //given
        MatchApiService matchApiService = new MatchApiService(okHttpClient, objectMapper);
        //when, then
        assertThrows(NullPointerException.class, () -> matchApiService.getMatchesFromApi(baseUrl));
    }
}