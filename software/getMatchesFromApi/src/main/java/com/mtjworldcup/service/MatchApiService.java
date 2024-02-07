package com.mtjworldcup.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtjworldcup.exception.HttpClientException;
import com.mtjworldcup.model.MatchApiResponse;
import com.mtjworldcup.model.MatchDto;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;

public class MatchApiService {

    private static final Logger log = LoggerFactory.getLogger(MatchApiService.class);
    private static final String RAPID_API_HOST = "api-football-v1.p.rapidapi.com";

    private final OkHttpClient okHttpClient;

    public MatchApiService(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
    }

    public List<MatchDto> getMatchesFromApi(String baseUrl) {
        final String rapidApiKey = System.getenv("RAPID_API_KEY");
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String from = now.plusDays(1).format(formatter);
        String to = now.plusDays(7).format(formatter);
        int season = now.getYear();
        Request request = new Request.Builder()
                .url(String.format("%s&from=%s&to=%s&season=%d", baseUrl, from, to, season))
                .get()
                .addHeader("X-RapidAPI-Key", rapidApiKey)
                .addHeader("X-RapidAPI-Host", RAPID_API_HOST)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            ResponseBody responseBody = response.body();

            if (responseBody != null) {
                String jsonString = responseBody.string();
                log.info("Response body: {}", jsonString);
                ObjectMapper objectMapper = new ObjectMapper();
                MatchApiResponse matchApiResponse = objectMapper.readValue(jsonString, MatchApiResponse.class);
                return matchApiResponse.getResponse();
            } else throw new NoSuchElementException("No body from Api call!");
        } catch (IOException ex) {
            log.error("Exception thrown by http call. Exception: {}. Cause: {}", ex, ex.getCause());
            throw new HttpClientException("IO Exception thrown by getMatchesFromApi method");
        }
    }
}
