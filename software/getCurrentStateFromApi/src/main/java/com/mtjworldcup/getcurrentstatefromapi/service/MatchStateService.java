package com.mtjworldcup.getcurrentstatefromapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtjworldcup.common.exception.HttpClientException;
import com.mtjworldcup.common.model.MatchApiResponse;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class MatchStateService {

    private static final Logger log = LoggerFactory.getLogger(MatchStateService.class);

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    public MatchStateService() {
        this.okHttpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public MatchStateService(OkHttpClient okHttpClient, ObjectMapper objectMapper) {
        this.okHttpClient = okHttpClient;
        this.objectMapper = objectMapper;
    }

    public MatchApiResponse getCurrentState(List<String> matchIds) {
        log.info("Fetching current state for match ids: {}", matchIds);
        final String rapidApiKey = System.getenv("RAPID_API_KEY");
        final String rapidApiHost = System.getenv("RAPID_API_HOST");
        final String baseUrl = System.getenv("BASE_API_URL");
        var currentStateRequest = new Request.Builder()
                .url(String.format("%s/fixtures?ids=%s", baseUrl, String.join("-", matchIds)))
                .get()
                .addHeader("X-RapidAPI-Key", rapidApiKey)
                .addHeader("X-RapidAPI-Host", rapidApiHost)
                .build();
        try(var response = okHttpClient.newCall(currentStateRequest).execute()) {
            String responseBody = Optional.of(response)
                    .map(Response::body)
                    .map(body -> {
                        try {
                            return body.string();
                        } catch (IOException e) {
                            return null;
                        }
                    })
                    .orElseThrow(() -> new HttpClientException("Response body is empty"));
            return objectMapper.readValue(responseBody, MatchApiResponse.class);
        } catch (IOException e) {
            throw new HttpClientException("Error while fetching match state. Cause: " + e.getMessage());
        }
    }
}
