package com.mtjworldcup.gettypersrank.model;

public record UserDto(String username, int correctTypings) implements Comparable<UserDto>{

    @Override
    public int compareTo(UserDto userDto) {
        return userDto.correctTypings() - this.correctTypings();
    }
}
