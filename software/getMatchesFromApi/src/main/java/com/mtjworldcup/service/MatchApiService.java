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
import java.util.List;
import java.util.NoSuchElementException;

public class MatchApiService {

    public List<MatchDto> getMatchesFromApi(LambdaLogger logger) {
        OkHttpClient client = new OkHttpClient();

        String rapidApiKey = System.getenv("RAPID_API_KEY");

        Request request = new Request.Builder()
                .url("https://api-football-v1.p.rapidapi.com/v3/fixtures?league=39&season=2023&from=2023-10-22&to=2023-10-28")
                .get()
                .addHeader("X-RapidAPI-Key", rapidApiKey)
                .addHeader("X-RapidAPI-Host", "api-football-v1.p.rapidapi.com")
                .build();

        try (Response response = client.newCall(request).execute()) {
            ResponseBody responseBody = response.body();

            if (responseBody != null) {
                String jsonString = responseBody.string();
                logger.log("response body: \n" + jsonString, LogLevel.INFO);
                // Parse JSON using Jackson
                ObjectMapper objectMapper = new ObjectMapper();
                MatchApiResponse matchApiResponse = objectMapper.readValue(jsonString, MatchApiResponse.class);

                return matchApiResponse.getResponse();
            } else throw new NoSuchElementException("No body from Api call!");
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException("IO Exception thrown by getMatchesFromApi method");
        }
    }
}
