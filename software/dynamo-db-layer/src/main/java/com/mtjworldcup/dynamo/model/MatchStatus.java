package com.mtjworldcup.dynamo.model;

public enum MatchStatus {
    SCHEDULED, IN_PROGRESS, FINISHED;

    public static MatchStatus fromLongName(String longName) {
        return longName.equals("Match Finished") ? FINISHED : SCHEDULED;
    }
}
