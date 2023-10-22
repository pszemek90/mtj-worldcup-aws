package com.mtjworldcup.service;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtjworldcup.model.MatchApiResponse;
import com.mtjworldcup.model.MatchDto;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;

public class MatchApiService {

    private static final String RAPID_API_HOST = "api-football-v1.p.rapidapi.com";
    private static final String BASE_API_URL = "https://api-football-v1.p.rapidapi.com/v3/fixtures?league=39&season=2023"; //todo think what to do with a season, maybe get rid of it?
    private static final String RAPID_API_KEY = System.getenv("RAPID_API_KEY");

    public List<MatchDto> getMatchesFromApi(LambdaLogger logger) {
        OkHttpClient client = new OkHttpClient();
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String from = now.format(formatter);
        String to = now.plusDays(7).format(formatter);
        Request request = new Request.Builder()
                .url(String.format("%s&from=%s&to=%s", BASE_API_URL, from, to))
                .get()
                .addHeader("X-RapidAPI-Key", RAPID_API_KEY)
                .addHeader("X-RapidAPI-Host", RAPID_API_HOST)
                .build();

        try (Response response = client.newCall(request).execute()) {
            ResponseBody responseBody = response.body();

            if (responseBody != null) {
                String jsonString = responseBody.string();
                logger.log("response body: \n" + jsonString, LogLevel.INFO);
                ObjectMapper objectMapper = new ObjectMapper();
                MatchApiResponse matchApiResponse = objectMapper.readValue(jsonString, MatchApiResponse.class);
                return matchApiResponse.getResponse();
            } else throw new NoSuchElementException("No body from Api call!");
        } catch (IOException ex) {
            logger.log(String.format("Exception thrown by http call. Exception: %s. Cause: %s", ex, ex.getCause()));
            throw new RuntimeException("IO Exception thrown by getMatchesFromApi method");
        }
    }
}
