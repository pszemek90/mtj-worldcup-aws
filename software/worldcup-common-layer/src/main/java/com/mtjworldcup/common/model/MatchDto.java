package com.mtjworldcup.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchDto {
    private Fixture fixture;
    private Teams teams;
    private Goals goals;

    public Fixture getFixture() {
        return fixture;
    }

    public void setFixture(Fixture fixture) {
        this.fixture = fixture;
    }

    public Teams getTeams() {
        return teams;
    }

    public void setTeams(Teams teams) {
        this.teams = teams;
    }

    public Goals getGoals() {
        return goals;
    }

    public void setGoals(Goals goals) {
        this.goals = goals;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Fixture {
        private Long id;
        private Long timestamp;
        private Status status;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return "Fixture{" +
                    "id=" + id +
                    ", timestamp=" + timestamp +
                    ", status=" + status +
                    '}';
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Status {
        @JsonProperty("short")
        private String shortName;

        @JsonProperty("long")
        private String longName;

        public void setShortName(String shortName) {
            this.shortName = shortName;
        }

        public String getShortName() {
            return shortName;
        }

        public String getLongName() {
            return longName;
        }

        public void setLongName(String longName) {
            this.longName = longName;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Teams {
        private Team home;
        private Team away;

        public Team getHome() {
            return home;
        }

        public void setHome(Team home) {
            this.home = home;
        }

        public Team getAway() {
            return away;
        }

        public void setAway(Team away) {
            this.away = away;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Team {
            private String name;

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            @Override
            public String toString() {
                return "Team{" +
                        "name='" + name + '\'' +
                        '}';
            }
        }

        @Override
        public String toString() {
            return "Teams{" +
                    "home=" + home +
                    ", away=" + away +
                    '}';
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Goals {
        private Integer home;
        private Integer away;

        public Integer getHome() {
            return home;
        }

        public void setHome(Integer home) {
            this.home = home;
        }

        public Integer getAway() {
            return away;
        }

        public void setAway(Integer away) {
            this.away = away;
        }

        @Override
        public String toString() {
            return "Goals{" +
                    "home=" + home +
                    ", away=" + away +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "MatchDto{" +
                "fixture=" + fixture +
                ", teams=" + teams +
                ", goals=" + goals +
                '}';
    }
}
