package com.mtjworldcup.model;

import com.mtjworldcup.common.model.TypingStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record Typings(Map<LocalDate, List<Typing>> typings) {
    public record Typing(String match, TypingStatus status) {
    }
}
