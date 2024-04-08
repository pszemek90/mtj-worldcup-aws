package com.mtjworldcup.getalltypings.model;

import java.time.LocalDate;

public record TypingDto(LocalDate date, String match, String user, String result, boolean isCorrect) {
}
