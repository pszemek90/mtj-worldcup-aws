package com.mtjworldcup.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtjworldcup.exception.HttpClientException;
import com.mtjworldcup.model.*;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class MatchApiService {

    private static final Logger log = LoggerFactory.getLogger(MatchApiService.class);
    private static final String RAPID_API_HOST = "api-football-v1.p.rapidapi.com";
    private static final int PREMIER_LEAGUE_ID = 39;

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    public MatchApiService(OkHttpClient okHttpClient, ObjectMapper objectMapper) {
        this.okHttpClient = okHttpClient;
        this.objectMapper = objectMapper;
    }

    public List<MatchDto> getMatchesFromApi(String baseUrl) {
        final String rapidApiKey = System.getenv("RAPID_API_KEY");
        LocalDate now = LocalDate.now();
        Request currentSeasonRequest = new Request.Builder()
                .url(String.format("%s/leagues?id=%d&current=true", baseUrl, PREMIER_LEAGUE_ID))
                .get()
                .addHeader("X-RapidAPI-Key", rapidApiKey)
                .addHeader("X-RapidAPI-Host", RAPID_API_HOST)
                .build();
        int currentSeason;
        try (Response response = okHttpClient.newCall(currentSeasonRequest).execute()) {
            String currentSeasonResponseString = Optional.ofNullable(response)
                    .map(Response::body)
                    .map(body -> {
                        try{
                            return body.string();
                        } catch (IOException e) {
                            return null;
                        }
                    })
                    .orElseThrow(() -> new HttpClientException("Response body is empty"));
            CurrentSeasonResponse currentSeasonResponse = objectMapper.readValue(currentSeasonResponseString, CurrentSeasonResponse.class);
            currentSeason = Optional.ofNullable(currentSeasonResponse)
                    .map(CurrentSeasonResponse::getResponse)
                    .map(Collection::stream)
                    .map(Stream::findFirst)
                    .flatMap(Function.identity())
                    .map(SeasonResponseDto::getSeasons)
                    .map(Collection::stream)
                    .map(Stream::findFirst)
                    .flatMap(Function.identity())
                    .map(SeasonDto::getYear)
                    .orElseThrow(() -> new NoSuchElementException("Current season not found"));

        } catch (IOException e) {
            throw new HttpClientException("Getting current season failed. Cause: " + e.getMessage());
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String from = now.plusDays(1).format(formatter);
        String to = now.plusDays(7).format(formatter);
        Request request = new Request.Builder()
                .url(String.format("%s/fixtures?league=39&from=%s&to=%s&season=%d", baseUrl, from, to, currentSeason))
                .get()
                .addHeader("X-RapidAPI-Key", rapidApiKey)
                .addHeader("X-RapidAPI-Host", RAPID_API_HOST)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            ResponseBody responseBody = response.body();

            if (responseBody != null) {
                String jsonString = responseBody.string();
                log.info("Response body: {}", jsonString);
                MatchApiResponse matchApiResponse = objectMapper.readValue(jsonString, MatchApiResponse.class);
                return matchApiResponse.getResponse();
            } else throw new NoSuchElementException("No body from Api call!");
        } catch (IOException ex) {
            log.error("Exception thrown by http call. Exception: {}. Cause: {}", ex, ex.getCause());
            throw new HttpClientException("IO Exception thrown by getMatchesFromApi method");
        }
    }
}
