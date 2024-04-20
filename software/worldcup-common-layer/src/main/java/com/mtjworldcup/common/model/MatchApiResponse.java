package com.mtjworldcup.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchApiResponse {
    private List<MatchDto> response;

    public List<MatchDto> getResponse() {
        return response;
    }

    public void setResponse(List<MatchDto> response) {
        this.response = response;
    }
}
