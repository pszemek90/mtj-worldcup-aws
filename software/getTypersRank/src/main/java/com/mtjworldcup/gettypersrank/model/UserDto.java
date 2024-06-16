package com.mtjworldcup.gettypersrank.model;

import java.math.BigDecimal;

public record UserDto(String username, int correctTypings, BigDecimal balance) implements Comparable<UserDto>{

    @Override
    public int compareTo(UserDto o) {
        if (this.correctTypings() == o.correctTypings()) {
            return o.balance().compareTo(this.balance());
        }
        return o.correctTypings() - this.correctTypings();
    }
}
