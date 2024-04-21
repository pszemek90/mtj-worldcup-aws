package com.mtjworldcup.getcurrentstatefromapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtjworldcup.common.model.MatchApiResponse;
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

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SystemStubsExtension.class)
class MatchStateServiceTest {

    @SystemStub
    private EnvironmentVariables environmentVariables;

    private MockWebServer mockWebServer;
    private static final OkHttpClient okHttpClient = new OkHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static String baseUrl;

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
    void shouldReturnSuccessfulResponse_WhenApiRespondsSuccessfully() throws Exception {
        //given
        environmentVariables.set("RAPID_API_KEY", "TEST");
        environmentVariables.set("RAPID_API_HOST", "TEST");
        environmentVariables.set("BASE_API_URL", baseUrl);
        String matchStateResponse = Files.readString(Path.of("src/test/resources/files/api-success-response.json"));
        MockResponse mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(matchStateResponse);
        mockWebServer.enqueue(mockResponse);
        List<String> matchIds = List.of("1", "2", "3");
        MatchStateService matchStateService = new MatchStateService(okHttpClient, objectMapper);
        //when
        MatchApiResponse matchApiResponse = matchStateService.getCurrentState(matchIds);
        //then
        assertNotNull(matchApiResponse);
    }

    @Test
    void shouldReturnOneMatch_WhenOneMatchStatusRequested() throws Exception {
        //given
        environmentVariables.set("RAPID_API_KEY", "TEST");
        environmentVariables.set("RAPID_API_HOST", "TEST");
        environmentVariables.set("BASE_API_URL", baseUrl);
        String matchStateResponse = Files.readString(Path.of("src/test/resources/files/api-success-response.json"));
        MockResponse mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(matchStateResponse);
        mockWebServer.enqueue(mockResponse);
        List<String> matchIds = List.of("1");
        MatchStateService matchStateService = new MatchStateService(okHttpClient, objectMapper);
        //when
        MatchApiResponse matchApiResponse = matchStateService.getCurrentState(matchIds);
        //then
        assertEquals(1, matchApiResponse.getResponse().size());
    }

}