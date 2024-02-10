package com.mtjworldcup.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrentSeasonResponse {
    private List<SeasonResponseDto> response;

    public List<SeasonResponseDto> getResponse() {
        return response;
    }

    public void setResponse(List<SeasonResponseDto> response) {
        this.response = response;
    }
}
