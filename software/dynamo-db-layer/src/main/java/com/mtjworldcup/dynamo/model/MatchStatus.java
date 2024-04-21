package com.mtjworldcup.dynamo.model;

public enum MatchStatus {
    SCHEDULED, IN_PROGRESS, FINISHED;

    public static MatchStatus fromShortName(String shortName) {
        return shortName.equals("FT") ? FINISHED : SCHEDULED;
    }
}
