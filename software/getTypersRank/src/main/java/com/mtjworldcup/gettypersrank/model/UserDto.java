package com.mtjworldcup.gettypersrank.model;

import java.math.BigDecimal;

public record UserDto(String username, int correctTypings, BigDecimal balance) implements Comparable<UserDto>{

    @Override
    public int compareTo(UserDto userDto) {
        return userDto.correctTypings() - this.correctTypings();
    }
}
