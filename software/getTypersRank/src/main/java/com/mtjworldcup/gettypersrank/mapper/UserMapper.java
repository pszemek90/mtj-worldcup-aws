package com.mtjworldcup.gettypersrank.mapper;

import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.gettypersrank.model.UserDto;
import java.util.List;

public class UserMapper {

  private UserMapper() {}

  public static UserDto toUserDto(Match match) {
    return new UserDto(match.getPrimaryId(), match.getCorrectTypings());
  }

  public static List<UserDto> toUserDto(List<Match> matches) {
    return matches.stream().map(UserMapper::toUserDto).toList();
  }
}
