package com.mtjworldcup.getmytypings.model;

import com.mtjworldcup.common.model.TypingStatus;

public record Typing(String homeTeam, String result, String awayTeam, TypingStatus status) {
}
