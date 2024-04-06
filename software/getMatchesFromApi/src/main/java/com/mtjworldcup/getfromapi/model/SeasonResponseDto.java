package com.mtjworldcup.getfromapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SeasonResponseDto {
    private List<SeasonDto> seasons;

    public List<SeasonDto> getSeasons() {
        return seasons;
    }

    public void setSeasons(List<SeasonDto> seasons) {
        this.seasons = seasons;
    }
}
